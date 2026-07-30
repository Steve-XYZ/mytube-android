package com.mytube.android.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.R
import com.mytube.android.ui.components.EmptyStateScreen
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenUrl: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.bookmarks.isEmpty() && uiState.history.isEmpty()) {
        EmptyStateScreen(
            title = stringResource(R.string.library_title),
            emptyTitle = stringResource(R.string.library_empty_title),
            emptyBody = stringResource(R.string.library_empty_body),
            icon = Icons.AutoMirrored.Filled.List,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.library_title),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (uiState.bookmarks.isNotEmpty()) {
            item {
                SectionTitle(
                    title = stringResource(R.string.library_bookmarks),
                    count = uiState.bookmarks.size,
                )
            }
            items(
                items = uiState.bookmarks,
                key = { bookmark -> "bookmark:${bookmark.url}" },
            ) { bookmark ->
                LibraryEntryCard(
                    title = bookmark.title,
                    url = bookmark.url,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onOpen = { onOpenUrl(bookmark.url) },
                    onDelete = { viewModel.removeBookmark(bookmark.url) },
                )
            }
        }

        if (uiState.history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(
                        title = stringResource(R.string.library_history),
                        count = uiState.history.size,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearHistory) {
                        Text(stringResource(R.string.library_clear_history))
                    }
                }
            }
            items(
                items = uiState.history,
                key = { historyEntry -> "history:${historyEntry.url}" },
            ) { historyEntry ->
                val resources = LocalResources.current
                val visited = remember(historyEntry.visitedAt) {
                    DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT,
                    ).format(Date(historyEntry.visitedAt))
                }
                LibraryEntryCard(
                    title = historyEntry.title,
                    url = historyEntry.url,
                    supportingText = "$visited · ${
                        resources.getQuantityString(
                            R.plurals.library_visits,
                            historyEntry.visitCount,
                            historyEntry.visitCount,
                        )
                    }",
                    onOpen = { onOpenUrl(historyEntry.url) },
                    onDelete = { viewModel.deleteHistoryEntry(historyEntry.url) },
                )
            }
        }

        item {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(20.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$title · $count",
        modifier = modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LibraryEntryCard(
    title: String,
    url: String,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (leadingIcon == null) 0.dp else 12.dp),
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
    }
}
