package com.mytube.android.data.browser

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey
    val url: String,
    val title: String,
    val visitedAt: Long,
    val visitCount: Int = 1,
)
