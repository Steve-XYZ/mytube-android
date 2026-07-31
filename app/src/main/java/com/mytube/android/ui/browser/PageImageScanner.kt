package com.mytube.android.ui.browser

import org.json.JSONArray
import org.json.JSONTokener

data class DetectedPageImage(
    val url: String,
    val alt: String,
    val width: Int,
    val height: Int,
)

object PageImageParser {
    fun parse(evaluateJavascriptResult: String): List<DetectedPageImage> {
        val decoded = JSONTokener(evaluateJavascriptResult).nextValue()
        val array = JSONArray(if (decoded is String) decoded else evaluateJavascriptResult)
        val seen = mutableSetOf<String>()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val url = item.optString("url")
                val width = item.optInt("width").coerceAtLeast(0)
                val height = item.optInt("height").coerceAtLeast(0)
                val meaningfulSize = width == 0 || height == 0 || width >= 50 && height >= 50
                if (!BrowserAddress.isSafeWebUrl(url) || !meaningfulSize || !seen.add(url)) continue
                add(
                    DetectedPageImage(
                        url = url,
                        alt = item.optString("alt").take(160),
                        width = width,
                        height = height,
                    ),
                )
            }
        }.sortedByDescending { image -> image.width.toLong() * image.height }
            .take(MaxImages)
    }

    private const val MaxImages = 100
}

internal const val PageImageScanScript = """
    (() => JSON.stringify(Array.from(document.images).slice(0, 250).map((image) => ({
        url: image.currentSrc || image.src || '',
        alt: image.alt || '',
        width: image.naturalWidth || 0,
        height: image.naturalHeight || 0
    }))))()
"""

internal fun buildImageGalleryHtml(images: List<DetectedPageImage>): String {
    val cards = images.joinToString(separator = "") { image ->
        val url = htmlEscape(image.url)
        val label = htmlEscape(
            image.alt.ifBlank {
                if (image.width > 0 && image.height > 0) {
                    "${image.width} × ${image.height}"
                } else {
                    "Page image"
                }
            },
        )
        """<a href="$url" aria-label="Save $label"><img src="$url" alt="$label" loading="lazy"><span>$label</span></a>"""
    }
    return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https:; style-src 'unsafe-inline'">
        <style>
        *{box-sizing:border-box}body{margin:0;padding:8px;background:transparent;font:13px sans-serif}
        main{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
        a{display:block;position:relative;height:160px;border-radius:14px;overflow:hidden;background:#222;text-decoration:none}
        img{width:100%;height:100%;object-fit:cover}span{position:absolute;left:0;right:0;bottom:0;padding:18px 8px 7px;color:white;
        background:linear-gradient(transparent,rgba(0,0,0,.78));white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
        </style></head><body><main>$cards</main></body></html>
    """.trimIndent()
}

private fun htmlEscape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        append(
            when (character) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> character
            },
        )
    }
}
