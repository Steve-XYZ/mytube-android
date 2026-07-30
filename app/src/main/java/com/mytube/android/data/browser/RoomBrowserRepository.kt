package com.mytube.android.data.browser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomBrowserRepository(
    private val dao: BrowserDao,
    private val now: () -> Long = System::currentTimeMillis,
) : BrowserRepository {

    private val historyWriteMutex = Mutex()

    override fun observeHistory(): Flow<List<HistoryEntry>> = dao.observeHistory()

    override fun observeBookmarks(): Flow<List<Bookmark>> = dao.observeBookmarks()

    override suspend fun recordVisit(url: String, title: String) {
        historyWriteMutex.withLock {
            val visitedAt = now()
            val updated = dao.updateHistoryVisit(url, title, visitedAt)
            if (updated == 0) {
                dao.insertHistory(
                    HistoryEntry(
                        url = url,
                        title = title,
                        visitedAt = visitedAt,
                    ),
                )
            }
            dao.pruneHistory(MaxHistoryEntries)
        }
    }

    override suspend fun addBookmark(url: String, title: String) {
        dao.upsertBookmark(
            Bookmark(
                url = url,
                title = title,
                createdAt = now(),
            ),
        )
    }

    override suspend fun removeBookmark(url: String) {
        dao.deleteBookmark(url)
    }

    override suspend fun deleteHistoryEntry(url: String) {
        dao.deleteHistoryEntry(url)
    }

    override suspend fun clearHistory() {
        dao.clearHistory()
    }

    private companion object {
        const val MaxHistoryEntries = 500
    }
}
