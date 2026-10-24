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

    @Test
    fun `media page detection follows navigation and clears stale media`() = runTest {
        val viewModel = createViewModel()
        val mediaUrl = "https://www.youtube.com/watch?v=abc"

        viewModel.onPageStarted(1, mediaUrl)

        assertEquals(
            DetectedMedia(
                url = mediaUrl,
                platform = "youtube",
                source = MediaDetectionSource.Page,
            ),
            viewModel.uiState.value.activeTab.detectedMedia,
        )

        viewModel.onPageStarted(1, "https://example.com/article")

        assertNull(viewModel.uiState.value.activeTab.detectedMedia)
    }

    @Test
    fun `direct resource detection is scoped to current page`() = runTest {
        val viewModel = createViewModel()
        val currentPage = "https://example.com/article"
        val stalePage = "https://example.com/previous"
        val resource = "https://cdn.example.com/video.mp4?token=abc"
        viewModel.onPageStarted(1, currentPage)

        viewModel.onMediaResourceDetected(1, stalePage, resource)
        assertNull(viewModel.uiState.value.activeTab.detectedMedia)

        viewModel.onMediaResourceDetected(1, currentPage, resource)

        assertEquals(
            DetectedMedia(
                url = resource,
                platform = "direct",
                source = MediaDetectionSource.Resource,
            ),
            viewModel.uiState.value.activeTab.detectedMedia,
        )
    }

    @Test
    fun `image scan results stay scoped to the page that requested them`() = runTest {
        val viewModel = createViewModel()
        val pageUrl = "https://example.com/gallery"
        val image = DetectedPageImage(
            url = "https://cdn.example.com/photo.jpg",
            alt = "Photo",
            width = 800,
            height = 600,
        )
        viewModel.onPageStarted(1, pageUrl)

        assertEquals(1L to pageUrl, viewModel.startImageScan())
        viewModel.completeImageScan(1, pageUrl, Result.success(listOf(image)))

        assertEquals(listOf(image), viewModel.uiState.value.imageGallery.images)
        assertTrue(viewModel.uiState.value.imageGallery.isVisible)

        viewModel.onPageStarted(1, "https://example.com/next")

        assertTrue(viewModel.uiState.value.imageGallery.images.isEmpty())
        assertTrue(!viewModel.uiState.value.imageGallery.isVisible)
    }

    @Test
    fun `lost renderer hides the tab until it is reloaded`() = runTest {
        val viewModel = createViewModel()
        val pageUrl = "https://example.com/video"
        viewModel.onPageStarted(1, pageUrl)
        viewModel.startImageScan()

        viewModel.onRendererGone(1, crashed = true)

        val tab = viewModel.uiState.value.activeTab
        assertTrue(tab.rendererGone)
        assertTrue(!tab.isLoading)
        assertEquals("This page crashed.", tab.errorMessage)
        assertEquals(pageUrl, tab.url)
        assertTrue(!viewModel.uiState.value.imageGallery.isVisible)

        viewModel.onRendererRecovered(1)

        val recovered = viewModel.uiState.value.activeTab
        assertTrue(!recovered.rendererGone)
        assertNull(recovered.errorMessage)
    }

    @Test
    fun `navigating a crashed tab shows it again`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPageStarted(1, "https://example.com")
        viewModel.onRendererGone(1, crashed = true)

        viewModel.requestNavigation("vimeo.com")

        assertTrue(!viewModel.uiState.value.activeTab.rendererGone)
        assertEquals("https://vimeo.com", viewModel.uiState.value.navigationRequest?.url)
    }

    @Test
    fun `renderer killed by the system explains the memory reclaim`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPageStarted(1, "https://example.com")

        viewModel.onRendererGone(1, crashed = false)

        assertEquals(
            "Android closed this page to free memory.",
            viewModel.uiState.value.activeTab.errorMessage,
        )
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
