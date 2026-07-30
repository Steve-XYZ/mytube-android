package com.mytube.android.testing

import com.mytube.android.data.browser.Bookmark
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.HistoryEntry
import com.mytube.android.data.settings.AppSettings
import com.mytube.android.data.settings.SettingsRepository
import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeBrowserRepository : BrowserRepository {
    val history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    override fun observeHistory(): Flow<List<HistoryEntry>> = history
    override fun observeBookmarks(): Flow<List<Bookmark>> = bookmarks

    override suspend fun recordVisit(url: String, title: String) {
        history.update { entries ->
            listOf(
                HistoryEntry(
                    url = url,
                    title = title,
                    visitedAt = 1,
                ),
            ) + entries.filterNot { it.url == url }
        }
    }

    override suspend fun addBookmark(url: String, title: String) {
        bookmarks.update { entries ->
            listOf(Bookmark(url, title, createdAt = 1)) +
                entries.filterNot { it.url == url }
        }
    }

    override suspend fun removeBookmark(url: String) {
        bookmarks.update { entries -> entries.filterNot { it.url == url } }
    }

    override suspend fun deleteHistoryEntry(url: String) {
        history.update { entries -> entries.filterNot { it.url == url } }
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
    }
}

class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings(),
) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)
    override val settings: Flow<AppSettings> = mutableSettings

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        mutableSettings.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setBlockThirdPartyCookies(block: Boolean) {
        mutableSettings.update { it.copy(blockThirdPartyCookies = block) }
    }

    override suspend fun setSaveBrowsingHistory(save: Boolean) {
        mutableSettings.update { it.copy(saveBrowsingHistory = save) }
    }
}
