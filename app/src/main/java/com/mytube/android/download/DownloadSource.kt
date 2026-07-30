package com.mytube.android.download

import java.net.URI

object DownloadSource {
    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null
        val candidate = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) ->
                "https://${trimmed.substringAfter("://")}"
            !trimmed.contains("://") && trimmed.contains('.') -> "https://$trimmed"
            else -> return null
        }
        return candidate.takeIf(::isSafe)
    }

    private fun isSafe(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)
}
