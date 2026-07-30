package com.mytube.android.download

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.downloadEngineDataStore by preferencesDataStore(
    name = "download_engine",
)

class YtDlpUpdateStore(
    private val context: Context,
) {
    suspend fun lastSuccessfulUpdateAt(): Long =
        context.downloadEngineDataStore.data.first()[LastUpdateKey] ?: 0L

    suspend fun recordSuccessfulUpdate(timestamp: Long) {
        context.downloadEngineDataStore.edit { preferences ->
            preferences[LastUpdateKey] = timestamp
        }
    }

    private companion object {
        val LastUpdateKey = longPreferencesKey("last_successful_ytdlp_update_at")
    }
}
