package com.mytube.android.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserAddressTest {

    @Test
    fun `normalizes domains and upgrades cleartext urls`() {
        assertEquals(
            "https://youtube.com/watch?v=123",
            BrowserAddress.normalize("youtube.com/watch?v=123"),
        )
        assertEquals(
            "https://example.com/media",
            BrowserAddress.normalize("http://example.com/media"),
        )
    }

    @Test
    fun `turns plain text into a search`() {
        assertEquals(
            "https://www.google.com/search?q=funny+cats",
            BrowserAddress.normalize("funny cats"),
        )
    }

    @Test
    fun `rejects blank input and unsafe schemes`() {
        assertNull(BrowserAddress.normalize("  "))
        assertNull(BrowserAddress.normalize("file:///data/local/file"))
        assertNull(BrowserAddress.normalize("javascript:alert(1)"))
        assertFalse(BrowserAddress.isSafeWebUrl("javascript:alert(1)"))
        assertFalse(BrowserAddress.isSafeWebUrl("file:///data/local/file"))
        assertFalse(BrowserAddress.isSafeWebUrl("https://user:pass@example.com"))
        assertTrue(BrowserAddress.isSafeWebUrl("https://example.com/path"))
    }
}
