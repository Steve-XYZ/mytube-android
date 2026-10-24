package com.mytube.android.ui.browser

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.mytube.android.MainActivity
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class BrowserRendererRecoveryTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun crashedPageShowsRecoveryAndReloadsIntoNewWebView() {
        composeRule.onNodeWithText("Vimeo").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(TimeoutMillis) { attachedWebView() != null }
        val crashedWebView = checkNotNull(attachedWebView())

        composeRule.runOnUiThread { crashedWebView.loadUrl("chrome://crash") }

        composeRule.waitUntil(TimeoutMillis) { hasText("This page crashed.") }
        composeRule.runOnIdle { assertNull(attachedWebView()) }

        composeRule.onNodeWithText("Try again").performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(TimeoutMillis) { attachedWebView() != null }
        composeRule.onNodeWithText("This page crashed.").assertDoesNotExist()
        composeRule.runOnIdle { assertNotSame(crashedWebView, attachedWebView()) }
    }

    private fun hasText(text: String): Boolean =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun attachedWebView(): WebView? {
        var found: WebView? = null
        composeRule.runOnUiThread {
            found = composeRule.activity.window.decorView.findWebView()
        }
        return found
    }

    private fun View.findWebView(): WebView? = when (this) {
        is WebView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findWebView() }
        else -> null
    }

    private companion object {
        const val TimeoutMillis = 20_000L
    }
}
