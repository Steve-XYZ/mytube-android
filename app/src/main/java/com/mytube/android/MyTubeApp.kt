package com.mytube.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.mytube.android.navigation.AppDestination
import com.mytube.android.navigation.BrowserDestination
import com.mytube.android.navigation.DownloadsDestination
import com.mytube.android.navigation.LibraryDestination
import com.mytube.android.navigation.SettingsDestination
import com.mytube.android.navigation.topLevelDestinations
import com.mytube.android.ui.browser.BrowserRoute
import com.mytube.android.ui.downloads.DownloadsScreen
import com.mytube.android.ui.library.LibraryScreen
import com.mytube.android.ui.settings.SettingsScreen
import com.mytube.android.ui.theme.ThemeMode

@Composable
fun MyTubeApp(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    val backStack = remember {
        listOf<NavKey>(BrowserDestination).toMutableStateList()
    }
    val currentDestination = backStack.last() as AppDestination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                            BrowserRoute()
                        }

                        DownloadsDestination -> NavEntry(key) {
                            DownloadsScreen()
                        }

                        LibraryDestination -> NavEntry(key) {
                            LibraryScreen()
                        }

                        SettingsDestination -> NavEntry(key) {
                            SettingsScreen(
                                themeMode = themeMode,
                                onThemeModeSelected = onThemeModeSelected,
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
