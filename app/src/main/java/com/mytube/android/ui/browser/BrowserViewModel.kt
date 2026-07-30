package com.mytube.android.ui.browser

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BrowserUiState(
    val address: String = "",
    val preparedAddress: String? = null,
)

class BrowserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState = _uiState.asStateFlow()

    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address, preparedAddress = null) }
    }

    fun prepareNavigation() {
        val normalizedAddress = normalizeAddress(_uiState.value.address) ?: return
        _uiState.update {
            it.copy(
                address = normalizedAddress,
                preparedAddress = normalizedAddress,
            )
        }
    }

    internal fun normalizeAddress(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return if (trimmed.contains('.') && !trimmed.contains(' ')) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
        }
    }
}
