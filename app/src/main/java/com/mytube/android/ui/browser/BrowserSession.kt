package com.mytube.android.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

data class BrowserSessionCallbacks(
    val onPageStarted: (tabId: Long, url: String) -> Unit = { _, _ -> },
    val onPageStateChanged: (
        tabId: Long,
        url: String?,
        title: String?,
        progress: Int,
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) -> Unit = { _, _, _, _, _, _ -> },
    val onPageFinished: (tabId: Long, url: String, title: String?) -> Unit = { _, _, _ -> },
    val onPageError: (tabId: Long, message: String) -> Unit = { _, _ -> },
    val onDownloadRequested: (url: String) -> Unit = {},
)

class BrowserSession(
    private val context: Context,
) {
    private val webViews = mutableMapOf<Long, WebView>()
    var callbacks = BrowserSessionCallbacks()
    var blockThirdPartyCookies: Boolean = true
        set(value) {
            field = value
            webViews.values.forEach(::applyCookiePolicy)
        }

    fun obtainWebView(tabId: Long): WebView {
        val webView = webViews.getOrPut(tabId) { createWebView(tabId) }
        (webView.parent as? ViewGroup)?.removeView(webView)
        return webView
    }

    fun hasTab(tabId: Long): Boolean = webViews.containsKey(tabId)

    fun loadUrl(tabId: Long, url: String): Boolean {
        if (!BrowserAddress.isSafeWebUrl(url)) {
            callbacks.onPageError(tabId, "Only secure HTTPS pages can be opened.")
            return false
        }
        obtainWebView(tabId).loadUrl(url)
        return true
    }

    fun goBack(tabId: Long) {
        webViews[tabId]?.takeIf(WebView::canGoBack)?.goBack()
    }

    fun goForward(tabId: Long) {
        webViews[tabId]?.takeIf(WebView::canGoForward)?.goForward()
    }

    fun reload(tabId: Long) {
        webViews[tabId]?.reload()
    }

    fun stopLoading(tabId: Long) {
        webViews[tabId]?.stopLoading()
    }

    fun activate(tabId: Long) {
        webViews.forEach { (id, webView) ->
            if (id == tabId) webView.onResume() else webView.onPause()
        }
    }

    fun pauseAll() {
        webViews.values.forEach(WebView::onPause)
    }

    fun destroyTab(tabId: Long) {
        webViews.remove(tabId)?.destroySafely()
    }

    fun removeMissingTabs(validTabIds: Set<Long>) {
        webViews.keys
            .filterNot(validTabIds::contains)
            .toList()
            .forEach(::destroyTab)
    }

    fun destroy() {
        webViews.values.forEach(WebView::destroySafely)
        webViews.clear()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    private fun createWebView(tabId: Long): WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            safeBrowsingEnabled = true
            mediaPlaybackRequiresUserGesture = true
        }
        removeJavascriptInterface("searchBoxJavaBridge_")
        removeJavascriptInterface("accessibility")
        removeJavascriptInterface("accessibilityTraversal")
        applyCookiePolicy(this)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val requestedUri = request.url
                val requestedUrl = requestedUri.toString()
                if (BrowserAddress.isSafeWebUrl(requestedUrl)) return false

                if (requestedUri.scheme.equals("http", ignoreCase = true) &&
                    !requestedUri.host.isNullOrBlank()
                ) {
                    val upgradedUrl = requestedUri.buildUpon()
                        .scheme("https")
                        .build()
                        .toString()
                    view.loadUrl(upgradedUrl)
                    return true
                }

                callbacks.onPageError(tabId, "This link type is not supported.")
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                callbacks.onPageStarted(tabId, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                callbacks.onPageFinished(tabId, url, view.title)
                publishState(tabId, view, progress = 100)
            }

            override fun doUpdateVisitedHistory(
                view: WebView,
                url: String,
                isReload: Boolean,
            ) {
                publishState(tabId, view, view.progress)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame) {
                    callbacks.onPageError(
                        tabId,
                        "The page could not be loaded. Check your connection and try again.",
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError,
            ) {
                handler.cancel()
                callbacks.onPageError(tabId, "The page has an invalid security certificate.")
            }

            @RequiresApi(Build.VERSION_CODES.O_MR1)
            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse,
            ) {
                callback.backToSafety(true)
                callbacks.onPageError(tabId, "Android Safe Browsing blocked this page.")
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                publishState(tabId, view, newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                publishState(tabId, view, view.progress)
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                request.deny()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback,
            ) {
                callback.invoke(origin, false, false)
            }
        }

        setDownloadListener { url, _, _, _, _ ->
            if (BrowserAddress.isSafeWebUrl(url)) {
                callbacks.onDownloadRequested(url)
            }
        }
    }

    private fun publishState(tabId: Long, webView: WebView, progress: Int) {
        callbacks.onPageStateChanged(
            tabId,
            webView.url,
            webView.title,
            progress,
            webView.canGoBack(),
            webView.canGoForward(),
        )
    }

    private fun applyCookiePolicy(webView: WebView) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, !blockThirdPartyCookies)
        }
    }
}

private fun WebView.destroySafely() {
    stopLoading()
    loadUrl("about:blank")
    clearHistory()
    (parent as? ViewGroup)?.removeView(this)
    removeAllViews()
    destroy()
}
