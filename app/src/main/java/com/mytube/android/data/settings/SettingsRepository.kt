package com.mytube.android.data.settings

import com.mytube.android.ui.theme.ThemeMode
import com.mytube.android.data.download.DownloadFormatPreset
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setBlockThirdPartyCookies(block: Boolean)
    suspend fun setSaveBrowsingHistory(save: Boolean)
    suspend fun setDetectPageImages(detect: Boolean)
    suspend fun setDefaultDownloadFormat(format: DownloadFormatPreset)
}
