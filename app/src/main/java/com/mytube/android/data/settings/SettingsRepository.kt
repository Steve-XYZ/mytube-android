package com.mytube.android.data.settings

import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setBlockThirdPartyCookies(block: Boolean)
    suspend fun setSaveBrowsingHistory(save: Boolean)
}
