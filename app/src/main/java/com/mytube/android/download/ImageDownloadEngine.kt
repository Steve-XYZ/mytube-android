package com.mytube.android.download

import android.webkit.CookieManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ImageDownloadEngine {
    suspend fun download(
        sourceUrl: String,
        refererUrl: String?,
        outputDirectory: File,
        onMetadata: suspend (title: String) -> Unit,
        onProgress: (progress: Int, etaSeconds: Long?) -> Unit,
    ): DownloadedMedia = withContext(Dispatchers.IO) {
        prepareDirectory(outputDirectory)
        val connection = openConnection(sourceUrl, refererUrl)
        try {
            val contentType = connection.contentType
                ?.substringBefore(';')
                ?.lowercase()
                .orEmpty()
            require(contentType.startsWith("image/")) {
                "The selected resource is not an image"
            }
            val contentLength = connection.contentLengthLong
            require(contentLength <= MaxImageBytes || contentLength < 0) {
                "The selected image is larger than 50 MB"
            }
            val fileName = imageFileName(
                sourceUrl = connection.url.toString(),
                contentType = contentType,
                contentDisposition = connection.getHeaderField("Content-Disposition"),
            )
            onMetadata(fileName.substringBeforeLast('.'))
            val output = File(outputDirectory, fileName)
            connection.inputStream.use { input ->
                output.outputStream().use { sink ->
                    val buffer = ByteArray(DefaultBufferSize)
                    var copied = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        require(copied <= MaxImageBytes) {
                            "The selected image is larger than 50 MB"
                        }
                        sink.write(buffer, 0, read)
                        if (contentLength > 0) {
                            onProgress(((copied * 100) / contentLength).toInt(), null)
                        }
                    }
                }
            }
            onProgress(100, null)
            DownloadedMedia(file = output, title = output.nameWithoutExtension)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(sourceUrl: String, refererUrl: String?): HttpURLConnection {
        var current = requireHttpsUrl(sourceUrl)
        repeat(MaxRedirects + 1) { redirectCount ->
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TimeoutMs
                readTimeout = TimeoutMs
                setRequestProperty("User-Agent", UserAgent)
                refererUrl?.takeIf(BrowserAddressSafety::isHttps)?.let {
                    setRequestProperty("Referer", it)
                }
                CookieManager.getInstance().getCookie(current)?.let {
                    setRequestProperty("Cookie", it)
                }
            }
            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: error("Image redirect is missing its destination")
                connection.disconnect()
                check(redirectCount < MaxRedirects) { "Too many image redirects" }
                current = requireHttpsUrl(URI(current).resolve(location).toString())
            } else {
                check(status in 200..299) { "Image request failed with HTTP $status" }
                return connection
            }
        }
        error("Too many image redirects")
    }

    private fun prepareDirectory(directory: File) {
        if (directory.exists()) directory.listFiles()?.forEach(File::deleteRecursively)
        else check(directory.mkdirs()) { "Unable to create image download directory" }
    }

    private fun requireHttpsUrl(value: String): String {
        require(BrowserAddressSafety.isHttps(value)) { "Only HTTPS images can be downloaded" }
        return value
    }

    private companion object {
        const val MaxImageBytes = 50L * 1024 * 1024
        const val MaxRedirects = 5
        const val TimeoutMs = 20_000
        const val DefaultBufferSize = 32 * 1024
        const val UserAgent = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 MyTube/0.5"
    }
}

private object BrowserAddressSafety {
    fun isHttps(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)
}

internal fun imageFileName(
    sourceUrl: String,
    contentType: String,
    contentDisposition: String?,
): String {
    val headerName = contentDisposition
        ?.substringAfter("filename=", "")
        ?.trim()
        ?.trim('"')
        ?.takeIf(String::isNotBlank)
    val pathName = runCatching { URI(sourceUrl).path.substringAfterLast('/') }
        .getOrNull()
        ?.takeIf(String::isNotBlank)
    val requested = (headerName ?: pathName ?: "image")
        .substringBefore('?')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(80)
        .ifBlank { "image" }
    val knownExtension = requested.substringAfterLast('.', "").lowercase()
        .takeIf {
            it in setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "heic", "heif", "svg")
        }
    if (knownExtension != null) return requested
    val extension = when (contentType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/avif" -> "avif"
        "image/heic", "image/heif" -> "heic"
        "image/svg+xml" -> "svg"
        else -> "jpg"
    }
    return "${requested.substringBeforeLast('.')}.$extension"
}
