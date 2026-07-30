package com.mytube.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mytube.android.data.settings.SettingsRepository
import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val blockThirdPartyCookies: Boolean = true,
    val saveBrowsingHistory: Boolean = true,
)

class AppViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState = settingsRepository.settings
        .map { settings ->
            AppUiState(
                themeMode = settings.themeMode,
                blockThirdPartyCookies = settings.blockThirdPartyCookies,
                saveBrowsingHistory = settings.saveBrowsingHistory,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState(),
        )

    fun selectTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    fun setBlockThirdPartyCookies(block: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockThirdPartyCookies(block)
        }
    }

    fun setSaveBrowsingHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveBrowsingHistory(save)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(settingsRepository) as T
        }
    }
}
