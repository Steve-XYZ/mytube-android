package com.mytube.android

import android.content.Context
import androidx.room.Room
import com.mytube.android.data.browser.AppDatabase
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.RoomBrowserRepository
import com.mytube.android.data.settings.DataStoreSettingsRepository
import com.mytube.android.data.settings.SettingsRepository

class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "mytube.db",
    ).build()

    val browserRepository: BrowserRepository =
        RoomBrowserRepository(database.browserDao())

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(context)
}
