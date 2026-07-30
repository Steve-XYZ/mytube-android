package com.mytube.android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.mytube.android.navigation.AppDestination
import com.mytube.android.navigation.BrowserDestination
import com.mytube.android.navigation.DownloadsDestination
import com.mytube.android.navigation.LibraryDestination
import com.mytube.android.navigation.SettingsDestination
import com.mytube.android.navigation.topLevelDestinations
import com.mytube.android.ui.AppUiState
import com.mytube.android.ui.browser.BrowserRoute
import com.mytube.android.ui.browser.BrowserSession
import com.mytube.android.ui.browser.BrowserViewModel
import com.mytube.android.ui.downloads.DownloadsRoute
import com.mytube.android.ui.downloads.DownloadsViewModel
import com.mytube.android.ui.library.LibraryScreen
import com.mytube.android.ui.library.LibraryViewModel
import com.mytube.android.ui.settings.SettingsScreen
import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun MyTubeApp(
    appUiState: AppUiState,
    browserViewModel: BrowserViewModel,
    downloadsViewModel: DownloadsViewModel,
    libraryViewModel: LibraryViewModel,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBlockThirdPartyCookiesChanged: (Boolean) -> Unit,
    onSaveBrowsingHistoryChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val browserSession = remember(context) { BrowserSession(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backStack = remember {
        listOf<NavKey>(BrowserDestination).toMutableStateList()
    }
    val currentDestination = backStack.last() as AppDestination
    val latestDestination by rememberUpdatedState(currentDestination)

    DisposableEffect(browserSession) {
        onDispose(browserSession::destroy)
    }
    DisposableEffect(browserSession, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (latestDestination == BrowserDestination) {
                        browserSession.activate(browserViewModel.uiState.value.activeTabId)
                    }
                }

                Lifecycle.Event.ON_STOP -> browserSession.pauseAll()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(currentDestination) {
        if (currentDestination != BrowserDestination) {
            browserSession.pauseAll()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MyTubeNavigationBar(
                currentDestination = currentDestination,
                onDestinationSelected = backStack::navigateTo,
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { key ->
                    when (key) {
                        BrowserDestination -> NavEntry(key) {
                            BrowserRoute(
                                viewModel = browserViewModel,
                                session = browserSession,
                                blockThirdPartyCookies =
                                    appUiState.blockThirdPartyCookies,
                                saveBrowsingHistory =
                                    appUiState.saveBrowsingHistory,
                                onMessage = { message ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                onDownloadRequested = { url ->
                                    downloadsViewModel.updateSourceUrl(url)
                                    backStack.navigateTo(DownloadsDestination)
                                },
                            )
                        }

                        DownloadsDestination -> NavEntry(key) {
                            DownloadsRoute(
                                viewModel = downloadsViewModel,
                                onOpen = { task ->
                                    val uri = task.outputUri?.let(Uri::parse)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, task.outputMimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        runCatching { context.startActivity(intent) }
                                            .onFailure {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "No app can open this media file.",
                                                    )
                                                }
                                            }
                                    }
                                },
                                onMessage = { message ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                            )
                        }

                        LibraryDestination -> NavEntry(key) {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onOpenUrl = { url ->
                                    browserViewModel.requestNavigation(url)
                                    backStack.navigateTo(BrowserDestination)
                                },
                            )
                        }

                        SettingsDestination -> NavEntry(key) {
                            SettingsScreen(
                                themeMode = appUiState.themeMode,
                                blockThirdPartyCookies =
                                    appUiState.blockThirdPartyCookies,
                                saveBrowsingHistory =
                                    appUiState.saveBrowsingHistory,
                                onThemeModeSelected = onThemeModeSelected,
                                onBlockThirdPartyCookiesChanged =
                                    onBlockThirdPartyCookiesChanged,
                                onSaveBrowsingHistoryChanged =
                                    onSaveBrowsingHistoryChanged,
                            )
                        }

                        else -> error("Unknown navigation key: $key")
                    }
                },
            )
        }
    }
}

@Composable
private fun MyTubeNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        topLevelDestinations.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = currentDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

private fun SnapshotStateList<NavKey>.navigateTo(destination: AppDestination) {
    val existingIndex = indexOf(destination)
    if (existingIndex >= 0) {
        while (lastIndex > existingIndex) {
            removeAt(lastIndex)
        }
    } else {
        add(destination)
    }
}
