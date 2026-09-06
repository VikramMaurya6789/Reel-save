package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {

    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    fun getAllReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE id = :id LIMIT 1")
    fun getReelByIdFlow(id: Long): Flow<ReelEntity?>

    @Query("SELECT * FROM reels WHERE id = :id LIMIT 1")
    suspend fun getReelById(id: Long): ReelEntity?

    @Query("SELECT * FROM reels WHERE reelId = :reelId AND status = 'COMPLETED' LIMIT 1")
    suspend fun findCompletedReelByReelId(reelId: String): ReelEntity?

    @Query("SELECT * FROM reels WHERE reelId = :reelId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatestReelByReelId(reelId: String): ReelEntity?

    @Query("SELECT * FROM reels WHERE username LIKE '%' || :query || '%' OR filename LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchReels(query: String): Flow<List<ReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(entity: ReelEntity): Long

    @Update
    suspend fun updateReel(entity: ReelEntity)

    @Query("UPDATE reels SET progress = :progress, downloadedBytes = :downloadedBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float, downloadedBytes: Long, status: String)

    @Query("UPDATE reels SET status = :status, videoUri = :videoUri, completedAt = :completedAt, fileSize = :fileSize WHERE id = :id")
    suspend fun markCompleted(id: Long, status: String, videoUri: String, completedAt: Long, fileSize: Long)

    @Query("UPDATE reels SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: Long, errorMessage: String)

    @Query("DELETE FROM reels WHERE id = :id")
    suspend fun deleteReelById(id: Long)

    @Query("DELETE FROM reels")
    suspend fun clearAll()
}
