package com.mytube.android.ui.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageImageParserTest {
    @Test
    fun scannerFiltersTinyUnsafeAndDuplicateImagesThenSortsByArea() {
        val javascriptResult = """
            "[{\"url\":\"https://cdn.example.com/medium.jpg\",\"alt\":\"Medium\",\"width\":400,\"height\":300},{\"url\":\"data:image/png;base64,abc\",\"alt\":\"Inline\",\"width\":500,\"height\":500},{\"url\":\"https://cdn.example.com/tiny.png\",\"alt\":\"Tiny\",\"width\":10,\"height\":10},{\"url\":\"https://cdn.example.com/large.jpg\",\"alt\":\"Large\",\"width\":1200,\"height\":800},{\"url\":\"https://cdn.example.com/large.jpg\",\"alt\":\"Duplicate\",\"width\":1200,\"height\":800}]"
        """.trimIndent()

        val images = PageImageParser.parse(javascriptResult)

        assertEquals(listOf("Large", "Medium"), images.map(DetectedPageImage::alt))
    }

    @Test
    fun galleryHtmlEscapesUntrustedImageMetadata() {
        val html = buildImageGalleryHtml(
            listOf(
                DetectedPageImage(
                    url = "https://cdn.example.com/a.jpg?x=1&y=2",
                    alt = "<script>alert('x')</script>",
                    width = 100,
                    height = 100,
                ),
            ),
        )

        assert(!html.contains("<script>alert"))
        assert(html.contains("&lt;script&gt;"))
        assert(html.contains("x=1&amp;y=2"))
    }
}
