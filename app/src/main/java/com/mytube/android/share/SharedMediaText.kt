package com.mytube.android.share

import com.mytube.android.download.DownloadSource
import com.mytube.android.download.MediaUrlClassifier

data class SharedDownloadRequest(
    val id: Long,
    val url: String?,
)

object SharedMediaText {
    private val webUrl = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val trailingPunctuation = charArrayOf('.', ',', ';', '!', ')', ']', '}')

    fun extractDownloadUrl(text: String?): String? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        val candidates = buildList {
            DownloadSource.normalize(trimmed)?.let(::add)
            webUrl.findAll(trimmed).forEach { match ->
                val candidate = match.value.trimEnd(*trailingPunctuation)
                DownloadSource.normalize(candidate)?.let(::add)
            }
        }.distinct()

        return candidates.firstOrNull(MediaUrlClassifier::isLikelyMediaUrl)
            ?: candidates.firstOrNull()
    }
}
