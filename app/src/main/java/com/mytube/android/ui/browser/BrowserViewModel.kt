package com.mytube.android.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mytube.android.data.browser.Bookmark
import com.mytube.android.data.browser.BrowserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserTab(
    val id: Long,
    val title: String = "New tab",
    val url: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val errorMessage: String? = null,
)

data class NavigationRequest(
    val id: Long,
    val tabId: Long,
    val url: String,
)

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab(id = 1)),
    val activeTabId: Long = 1,
    val address: String = "",
    val navigationRequest: NavigationRequest? = null,
    val bookmarkedUrls: Set<String> = emptySet(),
) {
    val activeTab: BrowserTab
        get() = tabs.first { it.id == activeTabId }
}

class BrowserViewModel(
    private val repository: BrowserRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(BrowserUiState())
    private var nextTabId = 2L
    private var nextNavigationRequestId = 1L

    val uiState = combine(
        mutableUiState,
        repository.observeBookmarks(),
    ) { state, bookmarks ->
        state.copy(bookmarkedUrls = bookmarks.mapTo(mutableSetOf(), Bookmark::url))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowserUiState(),
    )

    fun updateAddress(address: String) {
        mutableUiState.update { it.copy(address = address) }
    }

    fun requestNavigation(value: String = mutableUiState.value.address): Boolean {
        val url = BrowserAddress.normalize(value) ?: return false
        mutableUiState.update { state ->
            state.copy(
                address = url,
                navigationRequest = NavigationRequest(
                    id = nextNavigationRequestId++,
                    tabId = state.activeTabId,
                    url = url,
                ),
            )
        }
        return true
    }

    fun consumeNavigationRequest(requestId: Long) {
        mutableUiState.update { state ->
            if (state.navigationRequest?.id == requestId) {
                state.copy(navigationRequest = null)
            } else {
                state
            }
        }
    }

    fun addTab() {
        val state = mutableUiState.value
        if (state.tabs.size >= MaxTabs) return

        val tab = BrowserTab(id = nextTabId++)
        mutableUiState.update {
            it.copy(
                tabs = it.tabs + tab,
                activeTabId = tab.id,
                address = "",
                navigationRequest = null,
            )
        }
    }

    fun selectTab(tabId: Long) {
        val tab = mutableUiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        mutableUiState.update {
            it.copy(
                activeTabId = tabId,
                address = tab.url.orEmpty(),
                navigationRequest = null,
            )
        }
    }

    fun closeTab(tabId: Long) {
        val state = mutableUiState.value
        val closingIndex = state.tabs.indexOfFirst { it.id == tabId }
        if (closingIndex < 0) return

        if (state.tabs.size == 1) {
            val replacement = BrowserTab(id = nextTabId++)
            mutableUiState.value = BrowserUiState(
                tabs = listOf(replacement),
                activeTabId = replacement.id,
            )
            return
        }

        val remaining = state.tabs.filterNot { it.id == tabId }
        val activeId = if (state.activeTabId == tabId) {
            remaining[minOf(closingIndex, remaining.lastIndex)].id
        } else {
            state.activeTabId
        }
        val active = remaining.first { it.id == activeId }
        mutableUiState.update {
            it.copy(
                tabs = remaining,
                activeTabId = activeId,
                address = active.url.orEmpty(),
                navigationRequest = null,
            )
        }
    }

    fun onPageStarted(tabId: Long, url: String) {
        updateTab(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                progress = 0,
                errorMessage = null,
            )
        }
        updateAddressForActiveTab(tabId, url)
    }

    fun onPageStateChanged(
        tabId: Long,
        url: String?,
        title: String?,
        progress: Int,
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = url ?: tab.url,
                title = title?.takeIf(String::isNotBlank) ?: tab.title,
                progress = progress.coerceIn(0, 100),
                isLoading = progress < 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            )
        }
        url?.let { updateAddressForActiveTab(tabId, it) }
    }

    fun onPageFinished(
        tabId: Long,
        url: String,
        title: String?,
        saveHistory: Boolean,
    ) {
        val pageFailed = mutableUiState.value.tabs
            .firstOrNull { it.id == tabId }
            ?.errorMessage != null
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                title = title?.takeIf(String::isNotBlank) ?: url,
                isLoading = false,
                progress = 100,
            )
        }
        updateAddressForActiveTab(tabId, url)

        if (saveHistory && !pageFailed && BrowserAddress.isSafeWebUrl(url)) {
            viewModelScope.launch {
                repository.recordVisit(
                    url = url,
                    title = title?.takeIf(String::isNotBlank) ?: url,
                )
            }
        }
    }

    fun onPageError(tabId: Long, message: String) {
        updateTab(tabId) {
            it.copy(
                isLoading = false,
                errorMessage = message,
            )
        }
    }

    fun toggleBookmark() {
        val tab = mutableUiState.value.activeTab
        val url = tab.url?.takeIf(BrowserAddress::isSafeWebUrl) ?: return
        val isBookmarked = uiState.value.bookmarkedUrls.contains(url)

        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(url, tab.title)
            }
        }
    }

    fun deleteHistoryEntry(url: String) {
        viewModelScope.launch { repository.deleteHistoryEntry(url) }
    }

    private fun updateTab(tabId: Long, transform: (BrowserTab) -> BrowserTab) {
        mutableUiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) transform(tab) else tab
                },
            )
        }
    }

    private fun updateAddressForActiveTab(tabId: Long, address: String) {
        mutableUiState.update { state ->
            if (state.activeTabId == tabId) state.copy(address = address) else state
        }
    }

    class Factory(
        private val repository: BrowserRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BrowserViewModel::class.java))
            return BrowserViewModel(repository) as T
        }
    }

    companion object {
        const val MaxTabs = 8
    }
}
