package com.mytube.android.ui.downloads

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mytube.android.R
import com.mytube.android.ui.components.EmptyStateScreen

@Composable
fun DownloadsScreen() {
    EmptyStateScreen(
        title = stringResource(R.string.downloads_title),
        emptyTitle = stringResource(R.string.downloads_empty_title),
        emptyBody = stringResource(R.string.downloads_empty_body),
        icon = Icons.Filled.PlayArrow,
    )
}
