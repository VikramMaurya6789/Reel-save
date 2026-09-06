package com.example.domain.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.repository.ReelRepositoryImpl
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong("DOWNLOAD_ID", -1L)
        if (downloadId == -1L) return@withContext Result.failure()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ReelRepositoryImpl(database.reelDao())
        val entity = database.reelDao().getReelById(downloadId) ?: return@withContext Result.failure()

        val downloader = ReelDownloader(applicationContext, repository)
        downloader.startDownload(entity.toDomainModel())

        Result.success(workDataOf("DOWNLOAD_ID" to downloadId))
    }
}
