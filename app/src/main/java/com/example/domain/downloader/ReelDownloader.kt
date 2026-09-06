package com.example.domain.downloader

import android.content.Context
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.repository.ReelRepository
import com.example.util.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ReelDownloader(
    private val context: Context,
    private val repository: ReelRepository,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val _activeDownloadsMap = MutableStateFlow<Map<Long, DownloadItem>>(emptyMap())
    val activeDownloadsMap: StateFlow<Map<Long, DownloadItem>> = _activeDownloadsMap.asStateFlow()

    /**
     * Starts downloading the Reel, writing directly into MediaStore Movies/ReelSave.
     */
    fun startDownload(
        item: DownloadItem,
        onComplete: ((uri: String) -> Unit)? = null
    ): Long {
        val downloadId = item.id

        // Cancel any existing job for same id
        activeJobs[downloadId]?.cancel()

        val job = scope.launch {
            executeDownload(item, onComplete)
        }
        activeJobs[downloadId] = job
        return downloadId
    }

    private suspend fun executeDownload(
        item: DownloadItem,
        onComplete: ((uri: String) -> Unit)?
    ) {
        val downloadId = item.id
        var createdUri: android.net.Uri? = null
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null

        try {
            repository.updateProgress(downloadId, 0f, 0L, DownloadStatus.DOWNLOADING)
            updateActiveState(item.copy(status = DownloadStatus.DOWNLOADING, progress = 0f))

            // Open stream in MediaStore under Movies/ReelSave
            val streamPair = MediaStoreHelper.openVideoOutputStream(context, item.filename)
                ?: throw IllegalStateException("Could not create media file in Gallery.")

            val targetUri = streamPair.first
            createdUri = targetUri
            outputStream = streamPair.second

            val request = Request.Builder()
                .url(item.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from server.")
            val totalBytes = body.contentLength().let { if (it > 0) it else item.fileSize }
            inputStream = body.byteStream()

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateTime = System.currentTimeMillis()
            var lastReadForSpeed = 0L
            var speedText: String? = null

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                // Update progress every 300ms to avoid UI thrashing
                if (now - lastUpdateTime >= 300) {
                    val timeDelta = (now - lastUpdateTime) / 1000.0
                    val bytesDelta = totalRead - lastReadForSpeed
                    val bytesPerSec = if (timeDelta > 0) (bytesDelta / timeDelta).toLong() else 0L
                    speedText = FileUtils.formatSpeed(bytesPerSec)

                    val progress = if (totalBytes > 0) {
                        (totalRead.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                    } else {
                        0f
                    }

                    repository.updateProgress(downloadId, progress, totalRead, DownloadStatus.DOWNLOADING)
                    updateActiveState(
                        item.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = progress,
                            downloadedBytes = totalRead,
                            fileSize = totalBytes,
                            downloadSpeed = speedText
                        )
                    )

                    DownloadNotificationHelper.showProgressNotification(
                        context = context,
                        id = downloadId,
                        filename = item.filename,
                        progress = progress,
                        downloadedBytes = totalRead,
                        totalBytes = totalBytes
                    )

                    lastUpdateTime = now
                    lastReadForSpeed = totalRead
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null
            inputStream.close()
            inputStream = null

            // Finalize MediaStore entry (mark IS_PENDING = 0)
            MediaStoreHelper.finalizeVideoUri(context, targetUri)

            val finalSize = if (totalRead > 0) totalRead else totalBytes
            repository.markCompleted(downloadId, targetUri.toString(), finalSize)

            removeFromActiveState(downloadId)
            activeJobs.remove(downloadId)

            DownloadNotificationHelper.showCompletedNotification(
                context = context,
                id = downloadId,
                filename = item.filename,
                videoUri = targetUri.toString()
            )

            withContext(Dispatchers.Main) {
                onComplete?.invoke(targetUri.toString())
            }

        } catch (e: CancellationException) {
            // User cancelled download
            cleanup(outputStream, inputStream, createdUri)
            repository.updateProgress(downloadId, 0f, 0L, DownloadStatus.CANCELLED)
            removeFromActiveState(downloadId)
            activeJobs.remove(downloadId)
            DownloadNotificationHelper.cancelNotification(context, downloadId)
        } catch (e: Exception) {
            cleanup(outputStream, inputStream, createdUri)
            val friendlyError = if (e.message?.contains("ENOSPC", ignoreCase = true) == true) {
                "There's not enough storage space."
            } else {
                "Download failed. Try again."
            }
            repository.markFailed(downloadId, friendlyError)
            removeFromActiveState(downloadId)
            activeJobs.remove(downloadId)
            DownloadNotificationHelper.cancelNotification(context, downloadId)
        }
    }

    fun cancelDownload(downloadId: Long) {
        val job = activeJobs[downloadId]
        job?.cancel()
    }

    private fun cleanup(outputStream: OutputStream?, inputStream: InputStream?, uri: android.net.Uri?) {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        if (uri != null) {
            MediaStoreHelper.deleteVideo(context, uri.toString())
        }
    }

    private fun updateActiveState(item: DownloadItem) {
        val current = _activeDownloadsMap.value.toMutableMap()
        current[item.id] = item
        _activeDownloadsMap.value = current
    }

    private fun removeFromActiveState(id: Long) {
        val current = _activeDownloadsMap.value.toMutableMap()
        current.remove(id)
        _activeDownloadsMap.value = current
    }
}
