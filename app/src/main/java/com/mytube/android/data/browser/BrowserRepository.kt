package com.mytube.android.data.browser

import kotlinx.coroutines.flow.Flow

interface BrowserRepository {
    fun observeHistory(): Flow<List<HistoryEntry>>
    fun observeBookmarks(): Flow<List<Bookmark>>

    suspend fun recordVisit(url: String, title: String)
    suspend fun addBookmark(url: String, title: String)
    suspend fun removeBookmark(url: String)
    suspend fun deleteHistoryEntry(url: String)
    suspend fun clearHistory()
}
