package com.mytube.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.ui.AppViewModel
import com.mytube.android.share.SharedDownloadRequest
import com.mytube.android.share.SharedMediaText
import com.mytube.android.ui.browser.BrowserViewModel
import com.mytube.android.ui.downloads.DownloadsViewModel
import com.mytube.android.ui.library.LibraryViewModel
import com.mytube.android.ui.theme.MyTubeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    private val sharedDownloadRequest =
        MutableStateFlow<SharedDownloadRequest?>(null)
    private var nextSharedDownloadRequestId = 1L

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
        if (savedInstanceState == null) {
            acceptSharedIntent(intent)
        }

        setContent {
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
            val pendingShare by sharedDownloadRequest.collectAsStateWithLifecycle()

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
                    sharedDownloadRequest = pendingShare,
                    onSharedDownloadConsumed = ::consumeSharedDownloadRequest,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptSharedIntent(intent)
    }

    private fun acceptSharedIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND ||
            !intent.type.orEmpty().startsWith("text/")
        ) {
            return
        }
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        sharedDownloadRequest.value = SharedDownloadRequest(
            id = nextSharedDownloadRequestId++,
            url = SharedMediaText.extractDownloadUrl(text),
        )
    }

    private fun consumeSharedDownloadRequest(id: Long) {
        sharedDownloadRequest.update { request ->
            request?.takeUnless { it.id == id }
        }
    }
}
