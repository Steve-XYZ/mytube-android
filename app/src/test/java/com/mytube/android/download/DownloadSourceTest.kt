package com.mytube.android.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadSourceTest {
    @Test
    fun `normalizes secure media page urls`() {
        assertEquals(
            "https://youtube.com/watch?v=123",
            DownloadSource.normalize("youtube.com/watch?v=123"),
        )
        assertEquals(
            "https://example.com/video",
            DownloadSource.normalize("http://example.com/video"),
        )
    }

    @Test
    fun `rejects searches credentials and unsafe schemes`() {
        assertNull(DownloadSource.normalize("funny cats"))
        assertNull(DownloadSource.normalize("file:///tmp/video.mp4"))
        assertNull(DownloadSource.normalize("javascript:alert(1)"))
        assertNull(DownloadSource.normalize("https://user:pass@example.com/video"))
    }
}
