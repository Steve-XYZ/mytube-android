package com.mytube.android.data.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeHistory(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<Bookmark>>

    @Query(
        """
        UPDATE history
        SET title = :title, visitedAt = :visitedAt, visitCount = visitCount + 1
        WHERE url = :url
        """,
    )
    suspend fun updateHistoryVisit(url: String, title: String, visitedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query(
        """
        DELETE FROM history
        WHERE url NOT IN (
            SELECT url FROM history ORDER BY visitedAt DESC LIMIT :limit
        )
        """,
    )
    suspend fun pruneHistory(limit: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteHistoryEntry(url: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
