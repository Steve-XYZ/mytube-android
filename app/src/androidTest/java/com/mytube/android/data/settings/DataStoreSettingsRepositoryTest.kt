package com.mytube.android.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mytube.android.ui.theme.ThemeMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {

    @Test
    fun settingsArePersisted() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsFile = File(
            context.cacheDir,
            "settings-${UUID.randomUUID()}.preferences_pb",
        )
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { settingsFile },
        )
        val repository = DataStoreSettingsRepository(dataStore)

        repository.setThemeMode(ThemeMode.Dark)
        repository.setBlockThirdPartyCookies(false)
        repository.setSaveBrowsingHistory(false)

        val settings = repository.settings.first {
            it.themeMode == ThemeMode.Dark &&
                !it.blockThirdPartyCookies &&
                !it.saveBrowsingHistory
        }

        assertEquals(ThemeMode.Dark, settings.themeMode)
        assertFalse(settings.blockThirdPartyCookies)
        assertFalse(settings.saveBrowsingHistory)
    }
}
