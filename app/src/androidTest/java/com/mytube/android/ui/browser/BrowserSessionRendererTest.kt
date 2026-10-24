package com.mytube.android.ui.browser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserSessionRendererTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun rendererCrashDiscardsEveryTabWithoutKillingTheApp() {
        val session = BrowserSession(context)
        val goneTabs = ConcurrentHashMap<Long, Boolean>()
        val allTabsGone = CountDownLatch(2)
        lateinit var crashedWebView: Any

        instrumentation.runOnMainSync {
            session.callbacks = BrowserSessionCallbacks(
                onRendererGone = { tabId, crashed ->
                    goneTabs[tabId] = crashed
                    allTabsGone.countDown()
                },
            )
            session.obtainWebView(tabId = 2).loadUrl("about:blank")
            val webView = session.obtainWebView(tabId = 1)
            crashedWebView = webView
            webView.loadUrl("chrome://crash")
        }

        assertTrue(allTabsGone.await(20, TimeUnit.SECONDS))
        assertEquals(mapOf(1L to true, 2L to true), goneTabs.toMap())

        instrumentation.runOnMainSync {
            assertFalse(session.hasTab(1))
            assertFalse(session.hasTab(2))
            assertNotSame(crashedWebView, session.obtainWebView(tabId = 1))
            session.destroy()
        }
    }
}
