package com.mytube.android.ui.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserSessionSecurityTest {

    @Test
    fun webViewUsesHardenedDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val session = BrowserSession(context)
            val webView = session.obtainWebView(tabId = 1)
            val settings = webView.settings

            assertTrue(settings.javaScriptEnabled)
            assertTrue(settings.domStorageEnabled)
            assertTrue(settings.safeBrowsingEnabled)
            assertFalse(settings.allowFileAccess)
            assertFalse(settings.allowContentAccess)
            assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
            assertTrue(
                settings.mixedContentMode == WebSettings.MIXED_CONTENT_NEVER_ALLOW,
            )
            assertFalse(
                CookieManager.getInstance().acceptThirdPartyCookies(webView),
            )

            session.destroy()
        }
    }
}
