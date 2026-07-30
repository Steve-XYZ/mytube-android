package com.mytube.android.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedMediaTextTest {

    @Test
    fun `extracts a media url from shared title and text`() {
        assertEquals(
            "https://youtu.be/aqz-KE-bpKQ",
            SharedMediaText.extractDownloadUrl(
                "Big Buck Bunny https://youtu.be/aqz-KE-bpKQ",
            ),
        )
    }

    @Test
    fun `prefers a recognized media page when text contains multiple urls`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            SharedMediaText.extractDownloadUrl(
                "Source https://example.com Post https://www.youtube.com/watch?v=abc",
            ),
        )
    }

    @Test
    fun `accepts a safe schemeless url when shared alone`() {
        assertEquals(
            "https://vimeo.com/12345",
            SharedMediaText.extractDownloadUrl("vimeo.com/12345"),
        )
    }

    @Test
    fun `rejects text without a safe web url`() {
        assertNull(SharedMediaText.extractDownloadUrl("funny cats"))
        assertNull(SharedMediaText.extractDownloadUrl("file:///tmp/video.mp4"))
        assertNull(
            SharedMediaText.extractDownloadUrl(
                "https://user:password@example.com/video",
            ),
        )
    }
}
