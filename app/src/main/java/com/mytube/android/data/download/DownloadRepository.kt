package com.mytube.android.data.download

import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadTask>>
    suspend fun get(id: String): DownloadTask?
    suspend fun getQueued(): List<DownloadTask>
    suspend fun create(sourceUrl: String, formatPreset: DownloadFormatPreset): DownloadTask
    suspend fun claimForRun(id: String, maxConcurrent: Int): Boolean
    suspend fun updateTitle(id: String, title: String)
    suspend fun updateProgress(id: String, progress: Int, etaSeconds: Long?)
    suspend fun complete(
        id: String,
        outputUri: String,
        outputMimeType: String,
        fileName: String,
    ): Boolean
    suspend fun fail(id: String, message: String): Boolean
    suspend fun pause(id: String): Boolean
    suspend fun resume(id: String): Boolean
    suspend fun cancel(id: String): Boolean
    suspend fun delete(id: String): Boolean
}
