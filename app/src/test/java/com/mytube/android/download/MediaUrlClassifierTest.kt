package com.mytube.android.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlClassifierTest {

    @Test
    fun `recognizes supported media pages`() {
        val cases = mapOf(
            "https://www.youtube.com/watch?v=abc" to "youtube",
            "https://youtu.be/abc" to "youtube",
            "https://youtube.com/shorts/abc" to "youtube",
            "https://instagram.com/reel/abc/" to "instagram",
            "https://www.tiktok.com/@creator/video/123" to "tiktok",
            "https://x.com/creator/status/123" to "x",
            "https://fb.watch/abc/" to "facebook",
            "https://reddit.com/r/videos/comments/abc/title/" to "reddit",
            "https://vimeo.com/12345" to "vimeo",
            "https://soundcloud.com/artist/track" to "soundcloud",
        )

        cases.forEach { (url, platform) ->
            val classification = MediaUrlClassifier.classify(url)
            assertTrue(url, classification.isMediaPage)
            assertEquals(url, platform, classification.platform)
        }
    }

    @Test
    fun `rejects platform home and listing pages`() {
        listOf(
            "https://youtube.com",
            "https://youtube.com/feed/subscriptions",
            "https://instagram.com/explore",
            "https://tiktok.com/@creator",
            "https://x.com/home",
            "https://vimeo.com/categories",
        ).forEach { url ->
            assertFalse(url, MediaUrlClassifier.isLikelyMediaUrl(url))
        }
    }

    @Test
    fun `recognizes direct media resources and manifests`() {
        listOf(
            "https://cdn.example/video.mp4",
            "https://cdn.example/audio.M4A?token=abc",
            "https://cdn.example/master.m3u8#quality",
            "https://cdn.example/manifest.mpd?expires=1",
        ).forEach { url ->
            assertTrue(url, MediaUrlClassifier.isDirectMediaResourceUrl(url))
            assertEquals(
                "direct",
                MediaUrlClassifier.classify(url).platform,
            )
        }
    }

    @Test
    fun `does not classify unsafe or unrelated resources`() {
        listOf(
            "file:///tmp/video.mp4",
            "javascript:alert(1)",
            "https://cdn.example/segment.ts",
            "https://example.com/article",
            "not a url",
        ).forEach { url ->
            assertFalse(url, MediaUrlClassifier.isLikelyMediaUrl(url))
            assertFalse(url, MediaUrlClassifier.isDirectMediaResourceUrl(url))
        }
    }
}
