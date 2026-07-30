package com.mytube.android.ui.browser

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BrowserAddress {

    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (SchemePrefix.containsMatchIn(trimmed) &&
            !trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return null
        }

        val candidate = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) ->
                "https://${trimmed.substringAfter("://")}"
            looksLikeHost(trimmed) -> "https://$trimmed"
            else -> return searchUrl(trimmed)
        }

        return candidate.takeIf(::isSafeWebUrl)
    }

    fun isSafeWebUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)

    private fun looksLikeHost(value: String): Boolean =
        value.contains('.') && !value.any(Char::isWhitespace)

    private fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return "https://www.google.com/search?q=$encoded"
    }

    private val SchemePrefix = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
}
