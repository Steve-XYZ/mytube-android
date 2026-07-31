package com.mytube.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mytube.android.data.browser.Bookmark
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.HistoryEntry
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.DownloadState
import com.mytube.android.data.download.DownloadTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val downloads: List<DownloadTask> = emptyList(),
)

class LibraryViewModel(
    private val repository: BrowserRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val uiState = combine(
        repository.observeBookmarks(),
        repository.observeHistory(),
        downloadRepository.observeDownloads(),
    ) { bookmarks, history, downloads ->
        LibraryUiState(
            bookmarks = bookmarks,
            history = history,
            downloads = downloads.filter { it.state == DownloadState.Completed },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun removeBookmark(url: String) {
        viewModelScope.launch { repository.removeBookmark(url) }
    }

    fun deleteHistoryEntry(url: String) {
        viewModelScope.launch { repository.deleteHistoryEntry(url) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun removeDownload(id: String) {
        viewModelScope.launch { downloadRepository.delete(id) }
    }

    class Factory(
        private val repository: BrowserRepository,
        private val downloadRepository: DownloadRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
            return LibraryViewModel(repository, downloadRepository) as T
        }
    }
}
