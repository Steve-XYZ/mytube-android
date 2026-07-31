package com.mytube.android.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mytube.android.data.download.DownloadFormatPreset
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.DownloadTask
import com.mytube.android.data.settings.SettingsRepository
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

private data class DownloadInputState(
    val sourceUrl: String = "",
    val selectedFormat: DownloadFormatPreset? = null,
    val refererUrl: String? = null,
    val isSubmitting: Boolean = false,
)

class DownloadsViewModel(
    private val repository: DownloadRepository,
    private val coordinator: DownloadQueueController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val inputState = MutableStateFlow(DownloadInputState())
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    val uiState = combine(
        repository.observeDownloads(),
        inputState,
        settingsRepository.settings,
    ) { downloads, input, settings ->
        DownloadsUiState(
            downloads = downloads,
            sourceUrl = input.sourceUrl,
            formatPreset = input.selectedFormat ?: settings.defaultDownloadFormat,
            isSubmitting = input.isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState(),
    )

    fun updateSourceUrl(value: String) {
        inputState.update {
            it.copy(
                sourceUrl = value,
                selectedFormat = it.selectedFormat?.takeIf(DownloadFormatPreset::userSelectable),
                refererUrl = null,
            )
        }
    }

    fun selectFormat(preset: DownloadFormatPreset) {
        inputState.update { it.copy(selectedFormat = preset) }
    }

    fun prepareImage(sourceUrl: String, refererUrl: String) {
        inputState.value = DownloadInputState(
            sourceUrl = sourceUrl,
            selectedFormat = DownloadFormatPreset.ImageOriginal,
            refererUrl = refererUrl,
        )
    }

    fun enqueue(
        sourceUrl: String = uiState.value.sourceUrl,
        formatPreset: DownloadFormatPreset = uiState.value.formatPreset,
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
                coordinator.enqueue(
                    sourceUrl = normalized,
                    formatPreset = formatPreset,
                    refererUrl = inputState.value.refererUrl,
                )
            }.onSuccess {
                inputState.value = DownloadInputState()
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
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DownloadsViewModel::class.java))
            return DownloadsViewModel(repository, coordinator, settingsRepository) as T
        }
    }
}
