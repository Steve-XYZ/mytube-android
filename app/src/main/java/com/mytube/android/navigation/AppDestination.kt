package com.mytube.android.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.mytube.android.R

sealed interface AppDestination : NavKey {
    @get:StringRes
    val label: Int
    val icon: ImageVector
}

data object BrowserDestination : AppDestination {
    override val label = R.string.nav_browser
    override val icon = Icons.Filled.Home
}

data object DownloadsDestination : AppDestination {
    override val label = R.string.nav_downloads
    override val icon = Icons.Filled.PlayArrow
}

data object LibraryDestination : AppDestination {
    override val label = R.string.nav_library
    override val icon = Icons.AutoMirrored.Filled.List
}

data object SettingsDestination : AppDestination {
    override val label = R.string.nav_settings
    override val icon = Icons.Filled.Settings
}

val topLevelDestinations = listOf(
    BrowserDestination,
    DownloadsDestination,
    LibraryDestination,
    SettingsDestination,
)
