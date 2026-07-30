package com.mytube.android

import android.app.Application
import androidx.work.Configuration
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyTubeApplication : Application(), Configuration.Provider {
    val container by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setExecutor(Executors.newFixedThreadPool(MaxConcurrentDownloads))
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            container.downloadCoordinator.restoreQueued()
        }
    }

    companion object {
        const val MaxConcurrentDownloads = 2
    }
}
