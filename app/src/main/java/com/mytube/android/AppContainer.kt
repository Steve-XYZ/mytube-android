package com.mytube.android

import android.content.Context
import androidx.room.Room
import com.mytube.android.data.browser.AppDatabase
import com.mytube.android.data.browser.BrowserRepository
import com.mytube.android.data.browser.RoomBrowserRepository
import com.mytube.android.data.settings.DataStoreSettingsRepository
import com.mytube.android.data.settings.SettingsRepository
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.RoomDownloadRepository
import com.mytube.android.download.DownloadCoordinator
import com.mytube.android.download.MediaStorePublisher
import com.mytube.android.download.YoutubeDlEngine

class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "mytube.db",
    )
        .addMigrations(AppDatabase.Migration1To2)
        .build()

    val browserRepository: BrowserRepository =
        RoomBrowserRepository(database.browserDao())

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(context)

    val downloadRepository: DownloadRepository =
        RoomDownloadRepository(database, database.downloadDao())

    val youtubeDlEngine by lazy { YoutubeDlEngine(context) }

    val mediaStorePublisher by lazy { MediaStorePublisher(context) }

    val downloadCoordinator by lazy {
        DownloadCoordinator(
            context = context,
            repository = downloadRepository,
            engine = youtubeDlEngine,
        )
    }
}
