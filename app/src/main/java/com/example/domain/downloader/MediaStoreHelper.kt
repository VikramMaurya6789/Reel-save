package com.example.domain.downloader

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object MediaStoreHelper {

    private const val RELATIVE_SUBFOLDER = "Movies/ReelSave"
    private const val MIME_VIDEO_MP4 = "video/mp4"

    /**
     * Prepares an OutputStream in MediaStore under Movies/ReelSave.
     * Returns a pair of the created Content Uri and the open OutputStream.
     */
    fun openVideoOutputStream(
        context: Context,
        filename: String
    ): Pair<Uri, OutputStream>? {
        val resolver: ContentResolver = context.contentResolver

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, MIME_VIDEO_MP4)
                put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_SUBFOLDER)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null
            val outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                Pair(uri, outputStream)
            } else {
                resolver.delete(uri, null, null)
                null
            }
        } else {
            // Legacy storage (API 24-28)
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "ReelSave"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val targetFile = File(dir, filename)
            val outputStream = FileOutputStream(targetFile)
            val uri = Uri.fromFile(targetFile)
            Pair(uri, outputStream)
        }
    }

    /**
     * Finalizes MediaStore item (marks IS_PENDING = 0).
     */
    fun finalizeVideoUri(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (_: Exception) {
                // Ignore if update fails or not supported
            }
        }
    }

    /**
     * Deletes a video by its URI.
     */
    fun deleteVideo(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                val rows = context.contentResolver.delete(uri, null, null)
                rows > 0
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates an Intent to view the video in an external player.
     */
    fun createViewIntent(context: Context, uriString: String): Intent {
        val uri = Uri.parse(uriString)
        val shareableUri = getShareableUri(context, uri)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareableUri, MIME_VIDEO_MP4)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an Intent to share the video using Android Sharesheet.
     */
    fun createShareIntent(context: Context, uriString: String, caption: String? = null): Intent {
        val uri = Uri.parse(uriString)
        val shareableUri = getShareableUri(context, uri)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_VIDEO_MP4
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share Reel").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getShareableUri(context: Context, uri: Uri): Uri {
        return if (uri.scheme == "file") {
            try {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Exception) {
                uri
            }
        } else {
            uri
        }
    }
}
