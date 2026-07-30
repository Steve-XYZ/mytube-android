package com.mytube.android.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserViewModelTest {

    private val viewModel = BrowserViewModel()

    @Test
    fun `normalizes domains to https`() {
        assertEquals(
            "https://youtube.com/watch?v=123",
            viewModel.normalizeAddress("youtube.com/watch?v=123"),
        )
    }

    @Test
    fun `preserves fully qualified urls`() {
        assertEquals(
            "https://example.com/media",
            viewModel.normalizeAddress("https://example.com/media"),
        )
    }

    @Test
    fun `turns plain text into a search`() {
        assertEquals(
            "https://www.google.com/search?q=funny+cats",
            viewModel.normalizeAddress("funny cats"),
        )
    }

    @Test
    fun `rejects blank input`() {
        assertNull(viewModel.normalizeAddress("  "))
    }

    @Test
    fun `prepare navigation publishes normalized address`() {
        viewModel.updateAddress("vimeo.com")

        viewModel.prepareNavigation()

        assertEquals("https://vimeo.com", viewModel.uiState.value.preparedAddress)
    }
}
