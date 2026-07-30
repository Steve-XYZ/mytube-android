package com.mytube.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.ui.AppViewModel
import com.mytube.android.ui.browser.BrowserViewModel
import com.mytube.android.ui.downloads.DownloadsViewModel
import com.mytube.android.ui.library.LibraryViewModel
import com.mytube.android.ui.theme.MyTubeTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as MyTubeApplication).container

    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.Factory(container.settingsRepository)
    }
    private val browserViewModel: BrowserViewModel by viewModels {
        BrowserViewModel.Factory(container.browserRepository)
    }
    private val libraryViewModel: LibraryViewModel by viewModels {
        LibraryViewModel.Factory(container.browserRepository)
    }
    private val downloadsViewModel: DownloadsViewModel by viewModels {
        DownloadsViewModel.Factory(
            container.downloadRepository,
            container.downloadCoordinator,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

            MyTubeTheme(themeMode = uiState.themeMode) {
                MyTubeApp(
                    appUiState = uiState,
                    browserViewModel = browserViewModel,
                    downloadsViewModel = downloadsViewModel,
                    libraryViewModel = libraryViewModel,
                    onThemeModeSelected = appViewModel::selectTheme,
                    onBlockThirdPartyCookiesChanged =
                        appViewModel::setBlockThirdPartyCookies,
                    onSaveBrowsingHistoryChanged =
                        appViewModel::setSaveBrowsingHistory,
                )
            }
        }
    }
}
