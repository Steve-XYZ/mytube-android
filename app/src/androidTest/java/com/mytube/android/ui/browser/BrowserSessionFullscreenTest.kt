package com.mytube.android.ui.browser

import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserSessionFullscreenTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun pageFullscreenRequestIsExposedUntilPageExits() = onMainThread {
        val session = BrowserSession(context)
        val chromeClient = session.chromeClient(tabId = 1)
        val videoView = View(context)

        chromeClient.onShowCustomView(videoView, RecordingCallback())
        assertSame(videoView, session.fullscreenView.value)

        chromeClient.onHideCustomView()
        assertNull(session.fullscreenView.value)

        session.destroy()
    }

    @Test
    fun exitingFullscreenNotifiesThePage() = onMainThread {
        val session = BrowserSession(context)
        val callback = RecordingCallback()
        session.chromeClient(tabId = 1).onShowCustomView(View(context), callback)

        session.exitFullscreen()

        assertNull(session.fullscreenView.value)
        assertEquals(1, callback.hiddenCount)
        session.destroy()
    }

    @Test
    fun secondFullscreenRequestIsRejectedWhileOneIsShowing() = onMainThread {
        val session = BrowserSession(context)
        val firstView = View(context)
        val secondCallback = RecordingCallback()
        session.chromeClient(tabId = 1).onShowCustomView(firstView, RecordingCallback())

        session.chromeClient(tabId = 2).onShowCustomView(View(context), secondCallback)

        assertSame(firstView, session.fullscreenView.value)
        assertEquals(1, secondCallback.hiddenCount)
        session.destroy()
    }

    @Test
    fun closingTheFullscreenTabExitsFullscreen() = onMainThread {
        val session = BrowserSession(context)
        val callback = RecordingCallback()
        session.chromeClient(tabId = 1).onShowCustomView(View(context), callback)

        session.destroyTab(tabId = 2)
        assertEquals(0, callback.hiddenCount)

        session.destroyTab(tabId = 1)
        assertNull(session.fullscreenView.value)
        assertEquals(1, callback.hiddenCount)
        session.destroy()
    }

    private fun BrowserSession.chromeClient(tabId: Long): WebChromeClient =
        checkNotNull(obtainWebView(tabId).webChromeClient)

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private class RecordingCallback : WebChromeClient.CustomViewCallback {
        var hiddenCount = 0
            private set

        override fun onCustomViewHidden() {
            hiddenCount++
        }
    }
}
