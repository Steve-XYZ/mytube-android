package com.mytube.android.data.settings

import com.mytube.android.ui.theme.ThemeMode

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val blockThirdPartyCookies: Boolean = true,
    val saveBrowsingHistory: Boolean = true,
)
