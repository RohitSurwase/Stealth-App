package com.cosmos.unreddit.ui.redditsource

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.cosmos.unreddit.data.remote.BrowserUserAgent

/**
 * Shared WebView configuration for solving Redlib anti-bot challenges.
 *
 * Used by both [RedlibVerificationActivity] (full-screen) and
 * [RedlibVerificationFragment] (inline) so both send a byte-identical
 * User-Agent and share the same clearance cookies with the app's OkHttp
 * client (via [RedditCookieJar]).
 */
object RedlibWebView {

    /**
     * Matches the User-Agent used by OkHttp, otherwise Cloudflare's clearance
     * cookie obtained here would be rejected on the app's own requests.
     */
    fun applySettings(webView: WebView, context: Context, customUserAgent: String?) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // The challenge page never needs local files or content providers.
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = BrowserUserAgent.resolve(context, customUserAgent)
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    /**
     * Allow the cookies set by the challenge (including Cloudflare's
     * cross-domain `cf_clearance`) to reach this WebView and be picked up
     * later by OkHttp's cookie jar.
     */
    fun enableCookies(webView: WebView) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    /**
     * Serves the Anubis sha256 proof-of-work worker locally so the challenge
     * completes even when Cloudflare answers 403 for the worker script when
     * it is requested from an Android WebView.
     *
     * [onPageStarted] / [onPageFinished] drive the overlay progress indicator.
     */
    fun client(
        context: Context,
        onPageStarted: (() -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null,
    ): WebViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            onPageStarted?.invoke()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            onPageFinished?.invoke()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            // Transient resource errors are expected while a challenge loads;
            // ignore them instead of bailing out of the page.
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            // Ignore; the challenge reloads until it succeeds.
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val path = request?.url?.path ?: return null
            if (path.contains("/.within.website/x/cmd/anubis/static/js/worker/sha256-") &&
                path.endsWith(".mjs")
            ) {
                return WebResourceResponse(
                    "text/javascript",
                    "utf-8",
                    200,
                    "OK",
                    mapOf("Cache-Control" to "no-store"),
                    context.assets.open("anubis/sha256-webcrypto.mjs")
                )
            }
            return null
        }
    }
}
