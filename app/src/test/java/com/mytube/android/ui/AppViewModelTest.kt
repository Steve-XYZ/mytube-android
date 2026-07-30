package com.mytube.android.ui

import com.mytube.android.testing.FakeSettingsRepository
import com.mytube.android.testing.MainDispatcherRule
import com.mytube.android.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `settings changes are reflected in ui state`() = runTest {
        val viewModel = AppViewModel(FakeSettingsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.selectTheme(ThemeMode.Dark)
        viewModel.setBlockThirdPartyCookies(false)
        viewModel.setSaveBrowsingHistory(false)

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.blockThirdPartyCookies)
        assertFalse(viewModel.uiState.value.saveBrowsingHistory)
    }
}
