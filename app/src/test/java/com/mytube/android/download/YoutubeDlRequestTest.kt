package com.mytube.android.download

import com.mytube.android.data.download.DownloadFormatPreset
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeDlRequestTest {
    @Test
    fun `video preset selects and merges bounded video`() {
        val command = createDownloadRequest(
            sourceUrl = "https://example.com/video",
            formatPreset = DownloadFormatPreset.Video720p,
            outputDirectory = File("/tmp/download"),
        ).buildCommand()

        assertTrue(command.contains("bv*[height<=720]+ba/b[height<=720]"))
        assertFalse(command.contains("bv*[height<=720]+ba/b[height<=720]/b"))
        assertTrue(command.contains("--merge-output-format"))
        assertTrue(command.contains("mp4"))
        assertTrue(command.contains("--no-playlist"))
        assertFalse(command.contains("--extract-audio"))
    }

    @Test
    fun `audio preset extracts mp3`() {
        val command = createDownloadRequest(
            sourceUrl = "https://example.com/video",
            formatPreset = DownloadFormatPreset.AudioMp3,
            outputDirectory = File("/tmp/download"),
        ).buildCommand()

        assertTrue(command.contains("--extract-audio"))
        assertTrue(command.contains("--audio-format"))
        assertTrue(command.contains("mp3"))
        assertFalse(command.contains("--merge-output-format"))
    }

    @Test
    fun `mime types match published extensions`() {
        assertTrue(mimeTypeFor("mp4") == "video/mp4")
        assertTrue(mimeTypeFor("webm") == "video/webm")
        assertTrue(mimeTypeFor("mp3") == "audio/mpeg")
    }
}
