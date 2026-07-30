package com.mytube.android.data.browser

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val url: String,
    val title: String,
    val createdAt: Long,
)
