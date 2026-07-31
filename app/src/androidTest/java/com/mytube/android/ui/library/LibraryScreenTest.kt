package com.mytube.android.ui.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.mytube.android.data.browser.Bookmark
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.HistoryEntry
import com.mytube.android.ui.theme.MyTubeTheme
import com.mytube.android.ui.theme.ThemeMode
import com.mytube.android.data.download.DownloadFormatPreset
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.DownloadTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sameUrlCanAppearInBookmarksAndHistory() {
        val url = "https://example.com"
        val viewModel = LibraryViewModel(
            StubBrowserRepository(
                bookmarks = listOf(Bookmark(url, "Example", createdAt = 1)),
                history = listOf(HistoryEntry(url, "Example", visitedAt = 1)),
            ),
            StubDownloadRepository(),
        )

        composeRule.setContent {
            MyTubeTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenUrl = {},
                    onOpenDownload = {},
                )
            }
        }

        composeRule.onAllNodesWithText(url).assertCountEquals(2)
    }
}

private class StubDownloadRepository(
    downloads: List<DownloadTask> = emptyList(),
) : DownloadRepository {
    private val downloadFlow = MutableStateFlow(downloads)

    override fun observeDownloads(): Flow<List<DownloadTask>> = downloadFlow
    override suspend fun get(id: String): DownloadTask? =
        downloadFlow.value.firstOrNull { it.id == id }
    override suspend fun getQueued(): List<DownloadTask> = emptyList()
    override suspend fun create(
        sourceUrl: String,
        formatPreset: DownloadFormatPreset,
        refererUrl: String?,
    ): DownloadTask = error("Not used")
    override suspend fun claimForRun(id: String, maxConcurrent: Int) = false
    override suspend fun updateTitle(id: String, title: String) = Unit
    override suspend fun updateProgress(id: String, progress: Int, etaSeconds: Long?) = Unit
    override suspend fun complete(
        id: String,
        outputUri: String,
        outputMimeType: String,
        fileName: String,
    ) = false
    override suspend fun fail(id: String, message: String) = false
    override suspend fun pause(id: String) = false
    override suspend fun resume(id: String) = false
    override suspend fun cancel(id: String) = false
    override suspend fun delete(id: String) = false
}

private class StubBrowserRepository(
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
) : BrowserRepository {
    private val bookmarkFlow = MutableStateFlow(bookmarks)
    private val historyFlow = MutableStateFlow(history)

    override fun observeHistory(): Flow<List<HistoryEntry>> = historyFlow
    override fun observeBookmarks(): Flow<List<Bookmark>> = bookmarkFlow
    override suspend fun recordVisit(url: String, title: String) = Unit
    override suspend fun addBookmark(url: String, title: String) = Unit
    override suspend fun removeBookmark(url: String) = Unit
    override suspend fun deleteHistoryEntry(url: String) = Unit
    override suspend fun clearHistory() = Unit
}
