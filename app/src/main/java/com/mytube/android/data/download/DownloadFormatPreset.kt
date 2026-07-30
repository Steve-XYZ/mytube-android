package com.mytube.android.data.download

enum class DownloadFormatPreset(
    val displayName: String,
    val formatSelector: String,
    val audioOnly: Boolean = false,
) {
    BestVideo(
        displayName = "Best video",
        formatSelector = "bv*+ba/b",
    ),
    Video1080p(
        displayName = "Video · up to 1080p",
        formatSelector = "bv*[height<=1080]+ba/b[height<=1080]",
    ),
    Video720p(
        displayName = "Video · up to 720p",
        formatSelector = "bv*[height<=720]+ba/b[height<=720]",
    ),
    Video480p(
        displayName = "Video · up to 480p",
        formatSelector = "bv*[height<=480]+ba/b[height<=480]",
    ),
    AudioMp3(
        displayName = "Audio · MP3",
        formatSelector = "bestaudio/best",
        audioOnly = true,
    ),
}
