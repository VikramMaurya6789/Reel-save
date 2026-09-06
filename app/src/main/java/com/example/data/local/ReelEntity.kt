package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus

@Entity(
    tableName = "reels",
    indices = [Index(value = ["reelId"]), Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class ReelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reelId: String,
    val url: String,
    val filename: String,
    val thumbnailUrl: String? = null,
    val username: String? = null,
    val videoUri: String? = null,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f,
    val status: String = DownloadStatus.IDLE.name,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
) {
    fun toDomainModel(): DownloadItem {
        val parsedStatus = try {
            DownloadStatus.valueOf(status)
        } catch (_: Exception) {
            DownloadStatus.IDLE
        }
        return DownloadItem(
            id = id,
            reelId = reelId,
            url = url,
            filename = filename,
            thumbnailUrl = thumbnailUrl,
            username = username,
            videoUri = videoUri,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            progress = progress,
            status = parsedStatus,
            createdAt = createdAt,
            completedAt = completedAt,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomainModel(item: DownloadItem): ReelEntity {
            return ReelEntity(
                id = item.id,
                reelId = item.reelId,
                url = item.url,
                filename = item.filename,
                thumbnailUrl = item.thumbnailUrl,
                username = item.username,
                videoUri = item.videoUri,
                fileSize = item.fileSize,
                downloadedBytes = item.downloadedBytes,
                progress = item.progress,
                status = item.status.name,
                createdAt = item.createdAt,
                completedAt = item.completedAt,
                errorMessage = item.errorMessage
            )
        }
    }
}
