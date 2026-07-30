package com.mytube.android.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mytube.android.data.download.DownloadFormatPreset
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.DownloadTask
import com.mytube.android.download.DownloadQueueController
import com.mytube.android.download.DownloadSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloads: List<DownloadTask> = emptyList(),
    val sourceUrl: String = "",
    val formatPreset: DownloadFormatPreset = DownloadFormatPreset.Video720p,
    val isSubmitting: Boolean = false,
)

class DownloadsViewModel(
    private val repository: DownloadRepository,
    private val coordinator: DownloadQueueController,
) : ViewModel() {
    private val inputState = MutableStateFlow(DownloadsUiState())
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    val uiState = combine(
        repository.observeDownloads(),
        inputState,
    ) { downloads, input ->
        input.copy(downloads = downloads)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState(),
    )

    fun updateSourceUrl(value: String) {
        inputState.update { it.copy(sourceUrl = value) }
    }

    fun selectFormat(preset: DownloadFormatPreset) {
        inputState.update { it.copy(formatPreset = preset) }
    }

    fun enqueue(
        sourceUrl: String = inputState.value.sourceUrl,
        formatPreset: DownloadFormatPreset = inputState.value.formatPreset,
    ) {
        val normalized = DownloadSource.normalize(sourceUrl)
        if (normalized == null) {
            messageChannel.trySend("Enter a valid HTTPS media page.")
            return
        }
        if (inputState.value.isSubmitting) return
        inputState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching {
                coordinator.enqueue(normalized, formatPreset)
            }.onSuccess {
                inputState.update { it.copy(sourceUrl = "") }
                messageChannel.send("Download added to the queue.")
            }.onFailure {
                messageChannel.send("The download could not be queued.")
            }
            inputState.update { it.copy(isSubmitting = false) }
        }
    }

    fun pause(id: String) = perform {
        if (!coordinator.pause(id)) "The download could not be paused." else null
    }

    fun resume(id: String) = perform {
        if (!coordinator.resume(id)) "The download could not be resumed." else null
    }

    fun cancel(id: String) = perform {
        if (!coordinator.cancel(id)) "The download could not be cancelled." else null
    }

    fun delete(id: String) = perform {
        if (!coordinator.delete(id)) "Only finished downloads can be removed." else null
    }

    private fun perform(action: suspend () -> String?) {
        viewModelScope.launch {
            action()?.let { messageChannel.send(it) }
        }
    }

    class Factory(
        private val repository: DownloadRepository,
        private val coordinator: DownloadQueueController,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DownloadsViewModel::class.java))
            return DownloadsViewModel(repository, coordinator) as T
        }
    }
}
