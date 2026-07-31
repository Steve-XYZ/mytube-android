package com.mytube.android.download

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDownloadEngineTest {
    @Test
    fun `image filename uses safe server filename and inferred extension`() {
        assertEquals(
            "summer_photo.png",
            imageFileName(
                sourceUrl = "https://cdn.example.com/original",
                contentType = "image/png",
                contentDisposition = "attachment; filename=\"summer photo\"",
            ),
        )
    }

    @Test
    fun `image filename preserves a supported URL extension`() {
        assertEquals(
            "photo.webp",
            imageFileName(
                sourceUrl = "https://cdn.example.com/images/photo.webp?size=large",
                contentType = "image/webp",
                contentDisposition = null,
            ),
        )
    }
}
