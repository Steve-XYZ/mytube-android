package com.mytube.android.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: String): DownloadTask?

    @Query("SELECT * FROM downloads WHERE state = 'Queued' ORDER BY createdAt ASC")
    suspend fun getQueued(): List<DownloadTask>

    @Insert
    suspend fun insert(task: DownloadTask)

    @Query(
        """
        SELECT COUNT(*) FROM downloads
        WHERE state = 'Running' AND id != :id
        """,
    )
    suspend fun countOtherRunning(id: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM downloads
        WHERE state = 'Queued'
          AND (createdAt < :createdAt OR (createdAt = :createdAt AND id < :id))
        """,
    )
    suspend fun countOlderQueued(id: String, createdAt: Long): Int

    @Query(
        """
        UPDATE downloads
        SET state = :state, updatedAt = :updatedAt, errorMessage = NULL
        WHERE id = :id
        """,
    )
    suspend fun updateState(id: String, state: DownloadState, updatedAt: Long): Int

    @Query(
        """
        UPDATE downloads
        SET title = :title, updatedAt = :updatedAt
        WHERE id = :id AND state = 'Running'
        """,
    )
    suspend fun updateTitle(id: String, title: String, updatedAt: Long)

    @Query(
        """
        UPDATE downloads
        SET progress = :progress, etaSeconds = :etaSeconds, updatedAt = :updatedAt
        WHERE id = :id AND state = 'Running'
        """,
    )
    suspend fun updateProgress(
        id: String,
        progress: Int,
        etaSeconds: Long?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE downloads
        SET state = 'Completed', progress = 100, etaSeconds = NULL,
            outputUri = :outputUri, outputMimeType = :outputMimeType,
            fileName = :fileName, errorMessage = NULL, updatedAt = :updatedAt
        WHERE id = :id AND state = 'Running'
        """,
    )
    suspend fun markCompleted(
        id: String,
        outputUri: String,
        outputMimeType: String,
        fileName: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE downloads
        SET state = 'Failed', etaSeconds = NULL, errorMessage = :message,
            updatedAt = :updatedAt
        WHERE id = :id AND state IN ('Queued', 'Running')
        """,
    )
    suspend fun markFailed(id: String, message: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE downloads
        SET state = 'Queued', progress = 0, etaSeconds = NULL,
            errorMessage = NULL, updatedAt = :updatedAt
        WHERE id = :id AND state IN ('Paused', 'Failed')
        """,
    )
    suspend fun markQueued(id: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE downloads
        SET state = 'Paused', etaSeconds = NULL, updatedAt = :updatedAt
        WHERE id = :id AND state IN ('Queued', 'Running')
        """,
    )
    suspend fun markPaused(id: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE downloads
        SET state = 'Cancelled', etaSeconds = NULL, updatedAt = :updatedAt
        WHERE id = :id AND state NOT IN ('Completed', 'Cancelled')
        """,
    )
    suspend fun markCancelled(id: String, updatedAt: Long): Int

    @Query(
        """
        DELETE FROM downloads
        WHERE id = :id AND state IN ('Completed', 'Failed', 'Cancelled')
        """,
    )
    suspend fun deleteTerminal(id: String): Int
}
