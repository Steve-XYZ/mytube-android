package com.mytube.android.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mytube.android.R
import com.mytube.android.ui.components.EmptyStateScreen

@Composable
fun LibraryScreen() {
    EmptyStateScreen(
        title = stringResource(R.string.library_title),
        emptyTitle = stringResource(R.string.library_empty_title),
        emptyBody = stringResource(R.string.library_empty_body),
        icon = Icons.AutoMirrored.Filled.List,
    )
}
