package com.mytube.android.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.mytube.android.download.MediaUrlClassifier
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val onMediaResourceDetected: (
        tabId: Long,
        pageUrl: String,
        resourceUrl: String,
    ) -> Unit = { _, _, _ -> },
    val onRendererGone: (tabId: Long, crashed: Boolean) -> Unit = { _, _ -> },
)

private class FullscreenRequest(
    val tabId: Long,
    val callback: WebChromeClient.CustomViewCallback,
)

class BrowserSession(
    private val context: Context,
) {
    private val webViews = mutableMapOf<Long, WebView>()
    private val currentPageUrls = ConcurrentHashMap<Long, String>()
    private val _fullscreenView = MutableStateFlow<View?>(null)
    val fullscreenView: StateFlow<View?> = _fullscreenView.asStateFlow()
    private var fullscreenRequest: FullscreenRequest? = null
    @Volatile
    var callbacks = BrowserSessionCallbacks()
    var blockThirdPartyCookies: Boolean = true
        set(value) {
            field = value
            webViews.values.forEach(::applyCookiePolicy)
        }

    fun obtainWebView(tabId: Long): WebView {
        val webView = webViewFor(tabId)
        (webView.parent as? ViewGroup)?.removeView(webView)
        return webView
    }

    private fun webViewFor(tabId: Long): WebView =
        webViews.getOrPut(tabId) { createWebView(tabId) }

    fun hasTab(tabId: Long): Boolean = webViews.containsKey(tabId)

    fun loadUrl(tabId: Long, url: String): Boolean {
        if (!BrowserAddress.isSafeWebUrl(url)) {
            callbacks.onPageError(tabId, "Only secure HTTPS pages can be opened.")
            return false
        }
        webViewFor(tabId).loadUrl(url)
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

    fun scanPageImages(
        tabId: Long,
        onResult: (Result<List<DetectedPageImage>>) -> Unit,
    ) {
        val webView = webViews[tabId]
        if (webView == null) {
            onResult(Result.failure(IllegalStateException("The active page is unavailable")))
            return
        }
        webView.evaluateJavascript(PageImageScanScript) { result ->
            onResult(runCatching { PageImageParser.parse(result) })
        }
    }

    fun activate(tabId: Long) {
        webViews.forEach { (id, webView) ->
            if (id == tabId) webView.onResume() else webView.onPause()
        }
    }

    fun pauseAll() {
        webViews.values.forEach(WebView::onPause)
    }

    fun exitFullscreen() {
        val request = fullscreenRequest ?: return
        clearFullscreen()
        request.callback.onCustomViewHidden()
    }

    private fun clearFullscreen() {
        fullscreenRequest = null
        _fullscreenView.value = null
    }

    fun destroyTab(tabId: Long) {
        if (fullscreenRequest?.tabId == tabId) exitFullscreen()
        currentPageUrls.remove(tabId)
        webViews.remove(tabId)?.destroySafely()
    }

    fun removeMissingTabs(validTabIds: Set<Long>) {
        webViews.keys
            .filterNot(validTabIds::contains)
            .toList()
            .forEach(::destroyTab)
    }

    fun destroy() {
        exitFullscreen()
        webViews.values.forEach(WebView::destroySafely)
        webViews.clear()
        currentPageUrls.clear()
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
                currentPageUrls[tabId] = url
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
                currentPageUrls[tabId] = url
                publishState(tabId, view, view.progress)
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                val resourceUrl = request.url.toString()
                if (!request.isForMainFrame &&
                    request.method.equals("GET", ignoreCase = true) &&
                    MediaUrlClassifier.isDirectMediaResourceUrl(resourceUrl)
                ) {
                    currentPageUrls[tabId]?.let { pageUrl ->
                        callbacks.onMediaResourceDetected(
                            tabId,
                            pageUrl,
                            resourceUrl,
                        )
                    }
                }
                return null
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

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail,
            ): Boolean {
                if (webViews[tabId] === view) {
                    webViews.remove(tabId)
                    if (fullscreenRequest?.tabId == tabId) clearFullscreen()
                    callbacks.onRendererGone(tabId, detail.didCrash())
                }
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                return true
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

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (fullscreenRequest != null) {
                    callback.onCustomViewHidden()
                    return
                }
                fullscreenRequest = FullscreenRequest(tabId, callback)
                _fullscreenView.value = view
            }

            override fun onHideCustomView() {
                if (fullscreenRequest?.tabId == tabId) clearFullscreen()
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
