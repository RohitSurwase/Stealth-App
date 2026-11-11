package com.cosmos.unreddit.data.remote

import android.content.Context
import android.webkit.WebSettings

/**
 * User-Agent handling shared by the OkHttp client and the verification WebView.
 *
 * Cloudflare's clearance cookie is bound to the User-Agent (and IP) that solved
 * the challenge. The WebView and OkHttp must therefore send byte-identical
 * User-Agent strings. We also avoid pretending to be a desktop browser while
 * the underlying engine reports an Android platform through
 * `navigator.userAgentData`, which Turnstile treats as a bot signal.
 */
object BrowserUserAgent {

    /**
     * The WebView default UA contains markers (`; wv`, `Version/4.0`) that are
     * only used by embedded browsers. Strip them so it looks like Chrome and is
     * consistent with the engine's real version.
     */
    fun sanitize(userAgent: String): String {
        return userAgent
            .replace("; wv", "")
            .replace("Version/4.0 ", "")
            .replace("  ", " ")
            .trim()
    }

    fun fromWebView(context: Context): String {
        return sanitize(WebSettings.getDefaultUserAgent(context))
    }

    fun isMobile(userAgent: String): Boolean {
        return userAgent.contains("Android", ignoreCase = true) ||
            userAgent.contains("Mobile", ignoreCase = true)
    }
}
