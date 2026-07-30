package com.mytube.android.download

import java.net.URI

data class MediaUrlClassification(
    val platform: String,
    val isMediaPage: Boolean,
    val reason: String? = null,
)

object MediaUrlClassifier {
    private val directMediaExtensions =
        Regex(
            """\.(?:mp4|m4v|webm|mov|mkv|mp3|m4a|wav|flac|m3u8|mpd)(?:$|[?#])""",
            RegexOption.IGNORE_CASE,
        )

    fun classify(url: String): MediaUrlClassification {
        val parsed = runCatching { URI(url) }.getOrNull()
            ?: return unsupported("unknown", "Invalid URL.")
        val scheme = parsed.scheme?.lowercase()
        val host = parsed.host?.let(::normalizeHost)
            ?: return unsupported("unknown", "Invalid URL.")
        val path = normalizePath(parsed.rawPath.orEmpty())
        val pathAndQuery = buildString {
            append(path)
            parsed.rawQuery?.let {
                append('?')
                append(it)
            }
        }

        if (scheme != "http" && scheme != "https") {
            return unsupported(
                "unknown",
                "Only http and https URLs can be downloaded.",
            )
        }
        if (directMediaExtensions.containsMatchIn(pathAndQuery)) {
            return media("direct")
        }
        if (isYouTubeHost(host)) {
            return classifyYouTubeUrl(host, path, parsed)
        }
        if (host == "instagram.com") {
            return classifyInstagramUrl(path)
        }
        if (host == "tiktok.com" || host.endsWith(".tiktok.com")) {
            return classifyTikTokUrl(path)
        }
        if (host == "twitter.com" || host == "x.com") {
            return classifyStatusUrl("x", path)
        }
        if (host == "facebook.com" || host == "fb.watch" ||
            host.endsWith(".facebook.com")
        ) {
            return classifyFacebookUrl(host, path, parsed)
        }
        if (host == "reddit.com" || host.endsWith(".reddit.com")) {
            return classifyRedditUrl(path)
        }
        if (host == "vimeo.com" || host.endsWith(".vimeo.com")) {
            return classifyPathBasedPlatform(
                "vimeo",
                path,
                Regex("""^/(?:\d+|channels/[^/]+/\d+|groups/[^/]+/videos/\d+)"""),
            )
        }
        if (host == "dailymotion.com" || host.endsWith(".dailymotion.com")) {
            return classifyPathBasedPlatform(
                "dailymotion",
                path,
                Regex("""^/video/[^/]+"""),
            )
        }
        if (host == "twitch.tv" || host.endsWith(".twitch.tv")) {
            return classifyPathBasedPlatform(
                "twitch",
                path,
                Regex("""^/(?:videos/\d+|[^/]+/clip/[^/]+|clip/[^/]+)"""),
            )
        }
        if (host == "rumble.com" || host.endsWith(".rumble.com")) {
            return classifyPathBasedPlatform(
                "rumble",
                path,
                Regex("""^/v[^/]+"""),
            )
        }
        if (host == "bilibili.com" || host.endsWith(".bilibili.com")) {
            return classifyPathBasedPlatform(
                "bilibili",
                path,
                Regex("""^/video/[^/]+"""),
            )
        }
        if (host == "odysee.com" || host.endsWith(".odysee.com")) {
            return classifyPathBasedPlatform(
                "odysee",
                path,
                Regex("""^/@[^/]+/[^/]+"""),
            )
        }
        if (host == "soundcloud.com" || host.endsWith(".soundcloud.com")) {
            return classifyTrackLikeUrl("soundcloud", path)
        }
        if (host == "bandcamp.com" || host.endsWith(".bandcamp.com")) {
            return classifyPathBasedPlatform(
                "bandcamp",
                path,
                Regex("""^/track/[^/]+"""),
            )
        }

        return unsupported(
            "unknown",
            "This page is not recognized as a downloadable media page.",
        )
    }

    fun isLikelyMediaUrl(url: String): Boolean = classify(url).isMediaPage

    fun isDirectMediaResourceUrl(url: String): Boolean {
        val parsed = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        val pathAndQuery = buildString {
            append(parsed.rawPath.orEmpty())
            parsed.rawQuery?.let {
                append('?')
                append(it)
            }
        }
        return directMediaExtensions.containsMatchIn(pathAndQuery)
    }

    private fun classifyYouTubeUrl(
        host: String,
        path: String,
        parsed: URI,
    ): MediaUrlClassification {
        if (host == "youtu.be") {
            return if (path.length > 1) {
                media("youtube")
            } else {
                unsupported(
                    "youtube",
                    "Open a specific YouTube video first.",
                )
            }
        }
        if (path == "/watch" && parsed.hasQueryParameter("v")) {
            return media("youtube")
        }
        if (Regex("""^/(?:shorts|live)/[^/]+""").containsMatchIn(path)) {
            return media("youtube")
        }
        return unsupported(
            "youtube",
            "Open a specific YouTube video, Short, or live URL first.",
        )
    }

    private fun classifyInstagramUrl(path: String): MediaUrlClassification {
        if (Regex("""^/(?:p|reel|reels|tv)/[^/]+""").containsMatchIn(path) ||
            Regex("""^/stories/[^/]+/\d+""").containsMatchIn(path)
        ) {
            return media("instagram")
        }
        return unsupported(
            "instagram",
            "Open a specific Instagram post, reel, story, or video first.",
        )
    }

    private fun classifyTikTokUrl(path: String): MediaUrlClassification {
        if (Regex("""^/@[^/]+/video/\d+""").containsMatchIn(path) ||
            Regex("""^/(?:t|v|embed/v2)/[^/]+""").containsMatchIn(path)
        ) {
            return media("tiktok")
        }
        return unsupported("tiktok", "Open a specific TikTok video first.")
    }

    private fun classifyStatusUrl(
        platform: String,
        path: String,
    ): MediaUrlClassification {
        if (Regex("""^/(?:i/)?[^/]+/status/\d+""").containsMatchIn(path) ||
            Regex("""^/i/status/\d+""").containsMatchIn(path)
        ) {
            return media(platform)
        }
        return unsupported(platform, "Open a specific post/status URL first.")
    }

    private fun classifyFacebookUrl(
        host: String,
        path: String,
        parsed: URI,
    ): MediaUrlClassification {
        if (host == "fb.watch" && path.length > 1) {
            return media("facebook")
        }
        if (path == "/watch" && parsed.hasQueryParameter("v")) {
            return media("facebook")
        }
        if (Regex("""^/(?:reel|watch|share/v|[^/]+/videos)/[^/]+""")
                .containsMatchIn(path)
        ) {
            return media("facebook")
        }
        return unsupported(
            "facebook",
            "Open a specific Facebook watch, reel, or video URL first.",
        )
    }

    private fun classifyRedditUrl(path: String): MediaUrlClassification {
        if (Regex("""^/(?:r/[^/]+/)?comments/[^/]+""").containsMatchIn(path) ||
            Regex("""^/r/[^/]+/comments/[^/]+""").containsMatchIn(path)
        ) {
            return media("reddit")
        }
        return unsupported("reddit", "Open a specific Reddit post first.")
    }

    private fun classifyPathBasedPlatform(
        platform: String,
        path: String,
        pattern: Regex,
    ): MediaUrlClassification =
        if (pattern.containsMatchIn(path)) {
            media(platform)
        } else {
            unsupported(
                platform,
                "Open a specific $platform media URL first.",
            )
        }

    private fun classifyTrackLikeUrl(
        platform: String,
        path: String,
    ): MediaUrlClassification =
        if (path.split('/').count(String::isNotBlank) >= 2) {
            media(platform)
        } else {
            unsupported(
                platform,
                "Open a specific $platform track URL first.",
            )
        }

    private fun URI.hasQueryParameter(name: String): Boolean =
        rawQuery
            ?.split('&')
            ?.any { it.substringBefore('=').equals(name, ignoreCase = true) }
            ?: false

    private fun normalizeHost(hostname: String): String =
        hostname
            .lowercase()
            .removePrefix("www.")
            .removePrefix("m.")

    private fun normalizePath(path: String): String =
        path.trimEnd('/').ifEmpty { "/" }

    private fun media(platform: String) =
        MediaUrlClassification(platform = platform, isMediaPage = true)

    private fun unsupported(platform: String, reason: String) =
        MediaUrlClassification(
            platform = platform,
            isMediaPage = false,
            reason = reason,
        )

    private fun isYouTubeHost(host: String): Boolean =
        host == "youtube.com" || host == "youtu.be"
}
