package com.mytube.android.data.settings

import com.mytube.android.ui.theme.ThemeMode
import com.mytube.android.data.download.DownloadFormatPreset

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val blockThirdPartyCookies: Boolean = true,
    val saveBrowsingHistory: Boolean = true,
    val detectPageImages: Boolean = true,
    val defaultDownloadFormat: DownloadFormatPreset = DownloadFormatPreset.Video720p,
)
