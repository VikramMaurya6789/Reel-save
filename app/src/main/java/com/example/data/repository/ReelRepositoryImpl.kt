package com.example.data.repository

import com.example.data.local.ReelDao
import com.example.data.local.ReelEntity
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.repository.ReelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReelRepositoryImpl(
    private val reelDao: ReelDao
) : ReelRepository {

    override fun getAllReels(): Flow<List<DownloadItem>> {
        return reelDao.getAllReels().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCompletedDownloads(): Flow<List<DownloadItem>> {
        return reelDao.getCompletedReels().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getActiveDownloads(): Flow<List<DownloadItem>> {
        return reelDao.getActiveReels().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getReelById(id: Long): Flow<DownloadItem?> {
        return reelDao.getReelByIdFlow(id).map { it?.toDomainModel() }
    }

    override suspend fun findCompletedReelByReelId(reelId: String): DownloadItem? {
        return reelDao.findCompletedReelByReelId(reelId)?.toDomainModel()
    }

    override suspend fun findLatestReelByReelId(reelId: String): DownloadItem? {
        return reelDao.findLatestReelByReelId(reelId)?.toDomainModel()
    }

    override fun searchReels(query: String): Flow<List<DownloadItem>> {
        return reelDao.searchReels(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertReel(item: DownloadItem): Long {
        return reelDao.insertReel(ReelEntity.fromDomainModel(item))
    }

    override suspend fun updateReel(item: DownloadItem) {
        reelDao.updateReel(ReelEntity.fromDomainModel(item))
    }

    override suspend fun updateProgress(
        id: Long,
        progress: Float,
        downloadedBytes: Long,
        status: DownloadStatus
    ) {
        reelDao.updateProgress(id, progress, downloadedBytes, status.name)
    }

    override suspend fun markCompleted(id: Long, videoUri: String, fileSize: Long) {
        reelDao.markCompleted(
            id = id,
            status = DownloadStatus.COMPLETED.name,
            videoUri = videoUri,
            completedAt = System.currentTimeMillis(),
            fileSize = fileSize
        )
    }

    override suspend fun markFailed(id: Long, errorMessage: String) {
        reelDao.markFailed(id, errorMessage)
    }

    override suspend fun deleteReel(id: Long) {
        reelDao.deleteReelById(id)
    }

    override suspend fun clearAllHistory() {
        reelDao.clearAll()
    }
}
