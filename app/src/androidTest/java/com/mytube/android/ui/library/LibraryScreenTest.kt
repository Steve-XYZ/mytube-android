package com.mytube.android.ui.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.mytube.android.data.browser.Bookmark
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.HistoryEntry
import com.mytube.android.ui.theme.MyTubeTheme
import com.mytube.android.ui.theme.ThemeMode
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
        )

        composeRule.setContent {
            MyTubeTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenUrl = {},
                )
            }
        }

        composeRule.onAllNodesWithText(url).assertCountEquals(2)
    }
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
