package com.mytube.android.ui

import com.mytube.android.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {

    @Test
    fun `theme defaults to system and can be changed`() {
        val viewModel = AppViewModel()

        assertEquals(ThemeMode.System, viewModel.uiState.value.themeMode)

        viewModel.selectTheme(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
    }
}
