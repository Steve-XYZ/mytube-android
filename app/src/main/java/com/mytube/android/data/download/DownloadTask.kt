package com.mytube.android.data.download

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [
        Index("state"),
        Index("createdAt"),
    ],
)
data class DownloadTask(
    @PrimaryKey
    val id: String,
    val sourceUrl: String,
    val title: String,
    val formatPreset: DownloadFormatPreset,
    val state: DownloadState,
    val progress: Int = 0,
    val etaSeconds: Long? = null,
    val outputUri: String? = null,
    val outputMimeType: String? = null,
    val fileName: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
