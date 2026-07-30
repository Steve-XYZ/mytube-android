package com.mytube.android.data.browser

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BrowserRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).build()
        var now = 100L
        repository = RoomBrowserRepository(
            dao = database.browserDao(),
            now = { now++ },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun historyAndBookmarksPersistThroughRepository() = runTest {
        repository.recordVisit("https://example.com", "Example")
        repository.recordVisit("https://example.com", "Example updated")
        repository.addBookmark("https://example.com", "Example updated")

        val history = repository.observeHistory().first()
        val bookmarks = repository.observeBookmarks().first()

        assertEquals(1, history.size)
        assertEquals(2, history.single().visitCount)
        assertEquals("Example updated", history.single().title)
        assertEquals(1, bookmarks.size)

        repository.removeBookmark("https://example.com")
        repository.clearHistory()

        assertTrue(repository.observeBookmarks().first().isEmpty())
        assertTrue(repository.observeHistory().first().isEmpty())
    }
}
