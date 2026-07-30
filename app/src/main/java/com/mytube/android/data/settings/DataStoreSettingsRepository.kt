package com.mytube.android.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mytube.android.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    constructor(context: Context) : this(context.settingsDataStore)

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[ThemeKey]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.System,
                blockThirdPartyCookies = preferences[BlockThirdPartyCookiesKey] ?: true,
                saveBrowsingHistory = preferences[SaveBrowsingHistoryKey] ?: true,
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[ThemeKey] = themeMode.name }
    }

    override suspend fun setBlockThirdPartyCookies(block: Boolean) {
        dataStore.edit { it[BlockThirdPartyCookiesKey] = block }
    }

    override suspend fun setSaveBrowsingHistory(save: Boolean) {
        dataStore.edit { it[SaveBrowsingHistoryKey] = save }
    }

    private companion object {
        val ThemeKey = stringPreferencesKey("theme")
        val BlockThirdPartyCookiesKey = booleanPreferencesKey("block_third_party_cookies")
        val SaveBrowsingHistoryKey = booleanPreferencesKey("save_browsing_history")
    }
}
