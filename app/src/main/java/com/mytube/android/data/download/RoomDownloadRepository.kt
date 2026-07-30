package com.mytube.android.data.download

import androidx.room.withTransaction
import com.mytube.android.data.browser.AppDatabase
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class RoomDownloadRepository(
    private val database: AppDatabase,
    private val dao: DownloadDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : DownloadRepository {
    override fun observeDownloads(): Flow<List<DownloadTask>> = dao.observeDownloads()

    override suspend fun get(id: String): DownloadTask? = dao.get(id)

    override suspend fun getQueued(): List<DownloadTask> = dao.getQueued()

    override suspend fun create(
        sourceUrl: String,
        formatPreset: DownloadFormatPreset,
    ): DownloadTask {
        val timestamp = now()
        val task = DownloadTask(
            id = newId(),
            sourceUrl = sourceUrl,
            title = initialTitle(sourceUrl),
            formatPreset = formatPreset,
            state = DownloadState.Queued,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        dao.insert(task)
        return task
    }

    override suspend fun claimForRun(id: String, maxConcurrent: Int): Boolean =
        database.withTransaction {
            val task = dao.get(id) ?: return@withTransaction false
            if (task.state == DownloadState.Running) return@withTransaction true
            if (task.state != DownloadState.Queued) return@withTransaction false
            if (dao.countOtherRunning(id) >= maxConcurrent) return@withTransaction false
            if (dao.countOlderQueued(id, task.createdAt) > 0) return@withTransaction false

            dao.updateState(id, DownloadState.Running, now()) > 0
        }

    override suspend fun updateTitle(id: String, title: String) {
        dao.updateTitle(id, title, now())
    }

    override suspend fun updateProgress(id: String, progress: Int, etaSeconds: Long?) {
        dao.updateProgress(id, progress.coerceIn(0, 100), etaSeconds, now())
    }

    override suspend fun complete(
        id: String,
        outputUri: String,
        outputMimeType: String,
        fileName: String,
    ): Boolean = dao.markCompleted(
        id = id,
        outputUri = outputUri,
        outputMimeType = outputMimeType,
        fileName = fileName,
        updatedAt = now(),
    ) > 0

    override suspend fun fail(id: String, message: String): Boolean =
        dao.markFailed(id, message, now()) > 0

    override suspend fun pause(id: String): Boolean =
        dao.markPaused(id, now()) > 0

    override suspend fun resume(id: String): Boolean =
        dao.markQueued(id, now()) > 0

    override suspend fun cancel(id: String): Boolean =
        dao.markCancelled(id, now()) > 0

    override suspend fun delete(id: String): Boolean =
        dao.deleteTerminal(id) > 0

    private fun initialTitle(url: String): String = runCatching {
        URI(url).host?.removePrefix("www.")?.takeIf(String::isNotBlank)
    }.getOrNull() ?: "Download"
}
