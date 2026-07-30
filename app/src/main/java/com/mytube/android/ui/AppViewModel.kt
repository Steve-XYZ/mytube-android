package com.mytube.android.ui

import androidx.lifecycle.ViewModel
import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.System,
)

class AppViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    fun selectTheme(themeMode: ThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
    }
}
