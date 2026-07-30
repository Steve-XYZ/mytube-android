package com.mytube.android.ui.browser

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.mytube.android.ui.theme.MyTubeTheme
import com.mytube.android.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class BrowserContextualDownloadTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detectedMediaShowsContextualDownloadAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = BrowserSession(context)
        val mediaUrl = "https://www.youtube.com/watch?v=abc"

        composeRule.setContent {
            MyTubeTheme(themeMode = ThemeMode.Light) {
                BrowserScreen(
                    uiState = BrowserUiState(
                        tabs = listOf(
                            BrowserTab(
                                id = 1,
                                url = mediaUrl,
                                detectedMedia = DetectedMedia(
                                    url = mediaUrl,
                                    platform = "youtube",
                                    source = MediaDetectionSource.Page,
                                ),
                            ),
                        ),
                        address = mediaUrl,
                    ),
                    session = session,
                    onAddressChanged = {},
                    onGo = {},
                    onOpenStartingPoint = {},
                    onAddTab = {},
                    onSelectTab = {},
                    onCloseTab = {},
                    onBack = {},
                    onForward = {},
                    onReload = {},
                    onToggleBookmark = {},
                    onDownloadDetectedMedia = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Download detected media")
            .assertExists()

        composeRule.runOnUiThread(session::destroy)
    }
}
