package com.mytube.android.ui.downloads

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.R
import com.mytube.android.data.download.DownloadFormatPreset
import com.mytube.android.data.download.DownloadState
import com.mytube.android.data.download.DownloadTask

@Composable
fun DownloadsRoute(
    viewModel: DownloadsViewModel,
    onOpen: (DownloadTask) -> Unit,
    onMessage: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingEnqueue by remember {
        mutableStateOf<Pair<String, DownloadFormatPreset>?>(null)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val request = pendingEnqueue
        pendingEnqueue = null
        if (request == null) return@rememberLauncherForActivityResult
        val storageGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            results[Manifest.permission.WRITE_EXTERNAL_STORAGE] != false
        if (storageGranted) {
            viewModel.enqueue(request.first, request.second)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                results[Manifest.permission.POST_NOTIFICATIONS] == false
            ) {
                onMessage("Download started. Progress notifications are disabled.")
            }
        } else {
            onMessage("Storage permission is required on this Android version.")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect(onMessage)
    }

    DownloadsScreen(
        uiState = uiState,
        onSourceUrlChanged = viewModel::updateSourceUrl,
        onFormatSelected = viewModel::selectFormat,
        onEnqueue = {
            val requiredPermissions = buildList {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (requiredPermissions.isEmpty()) {
                viewModel.enqueue()
            } else {
                pendingEnqueue = uiState.sourceUrl to uiState.formatPreset
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onCancel = viewModel::cancel,
        onDelete = viewModel::delete,
        onOpen = onOpen,
    )
}

@Composable
fun DownloadsScreen(
    uiState: DownloadsUiState,
    onSourceUrlChanged: (String) -> Unit,
    onFormatSelected: (DownloadFormatPreset) -> Unit,
    onEnqueue: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (DownloadTask) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.downloads_title),
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = uiState.sourceUrl,
            onValueChange = onSourceUrlChanged,
            label = { Text(stringResource(R.string.downloads_url_label)) },
            placeholder = { Text("https://…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadFormatPreset.entries.forEach { preset ->
                FilterChip(
                    selected = uiState.formatPreset == preset,
                    onClick = { onFormatSelected(preset) },
                    label = { Text(preset.displayName) },
                )
            }
        }
        Button(
            onClick = onEnqueue,
            enabled = !uiState.isSubmitting && uiState.sourceUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (uiState.isSubmitting) {
                    stringResource(R.string.downloads_adding)
                } else {
                    stringResource(R.string.downloads_add)
                },
            )
        }
        Text(
            text = stringResource(R.string.downloads_queue),
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (uiState.downloads.isEmpty()) {
            Text(
                text = stringResource(R.string.downloads_empty_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = uiState.downloads,
                    key = DownloadTask::id,
                ) { task ->
                    DownloadCard(
                        task = task,
                        onPause = { onPause(task.id) },
                        onResume = { onResume(task.id) },
                        onCancel = { onCancel(task.id) },
                        onDelete = { onDelete(task.id) },
                        onOpen = { onOpen(task) },
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${task.formatPreset.displayName} · ${task.state.displayName()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                DownloadActions(
                    task = task,
                    onPause = onPause,
                    onResume = onResume,
                    onCancel = onCancel,
                    onDelete = onDelete,
                    onOpen = onOpen,
                )
            }
            if (task.state == DownloadState.Running || task.state == DownloadState.Queued) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                val progressText = buildString {
                    append("${task.progress}%")
                    task.etaSeconds?.let { append(" · ${it}s remaining") }
                }
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            task.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            task.fileName?.let { fileName ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DownloadActions(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    Row {
        when (task.state) {
            DownloadState.Running, DownloadState.Queued -> {
                TextButton(onClick = onPause) {
                    Text("Pause")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
            DownloadState.Paused, DownloadState.Failed -> {
                IconButton(onClick = onResume) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Resume")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
            DownloadState.Completed -> {
                TextButton(onClick = onOpen) {
                    Text("Open")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
            DownloadState.Cancelled -> {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}

private fun DownloadState.displayName(): String = when (this) {
    DownloadState.Queued -> "Queued"
    DownloadState.Running -> "Downloading"
    DownloadState.Paused -> "Paused"
    DownloadState.Completed -> "Completed"
    DownloadState.Failed -> "Failed"
    DownloadState.Cancelled -> "Cancelled"
}
