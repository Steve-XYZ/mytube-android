package com.mytube.android.ui.browser

import com.mytube.android.testing.FakeBrowserRepository
import com.mytube.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `navigation request targets active tab with normalized url`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateAddress("vimeo.com")
        viewModel.requestNavigation()

        val request = viewModel.uiState.value.navigationRequest
        assertNotNull(request)
        assertEquals(1L, request?.tabId)
        assertEquals("https://vimeo.com", request?.url)

        viewModel.consumeNavigationRequest(requireNotNull(request).id)

        assertNull(viewModel.uiState.value.navigationRequest)
    }

    @Test
    fun `tabs can be added selected and closed without leaving an empty stack`() = runTest {
        val viewModel = createViewModel()

        viewModel.addTab()
        val secondTabId = viewModel.uiState.value.activeTabId
        assertEquals(2, viewModel.uiState.value.tabs.size)

        viewModel.closeTab(secondTabId)
        assertEquals(1, viewModel.uiState.value.tabs.size)
        assertEquals(1L, viewModel.uiState.value.activeTabId)

        viewModel.closeTab(1L)
        assertEquals(1, viewModel.uiState.value.tabs.size)
        assertEquals(3L, viewModel.uiState.value.activeTabId)
    }

    @Test
    fun `failed pages are not added to history`() = runTest {
        val repository = FakeBrowserRepository()
        val viewModel = createViewModel(repository)
        val url = "https://unavailable.example"

        viewModel.onPageStarted(1, url)
        viewModel.onPageError(1, "Unavailable")
        viewModel.onPageFinished(1, url, "Unavailable", saveHistory = true)

        assertTrue(repository.history.value.isEmpty())
    }

    private fun kotlinx.coroutines.test.TestScope.createViewModel(): BrowserViewModel {
        return createViewModel(FakeBrowserRepository())
    }

    private fun kotlinx.coroutines.test.TestScope.createViewModel(
        repository: FakeBrowserRepository,
    ): BrowserViewModel {
        val viewModel = BrowserViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        return viewModel
    }
}
