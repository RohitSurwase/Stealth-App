package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import android.webkit.CookieManager

/**
 * Shares cookies between the app's OkHttp clients and the system [CookieManager]
 * (which the verification WebView populates), and injects the platform cookies
 * that each backend reads from the request.
 *
 * - `reddit.com` reads `over18` to decide whether NSFW posts are returned.
 * - Redlib reads `show_nsfw` the same way (its `Preferences` are built from the
 *   request cookies, never from the query string). Redlib hides every listing
 *   when that cookie is absent, so an app whose subscriptions are NSFW would
 *   otherwise get an empty feed.
 *
 * The [showNsfw] provider is evaluated per request, so toggling the app's
 * "Show NSFW content" preference applies on the next load without a restart.
 */
class RedditCookieJar(
    private val showNsfw: () -> Boolean = { false },
) : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val savedCookies = CookieManager.getInstance().getCookie(url.toString())
        if (savedCookies != null) {
            // CookieManager returns "name=value; name2=value2". Ignore any
            // fragment OkHttp cannot parse instead of crashing the request.
            for (cookieString in savedCookies.split(';')) {
                val cookie = Cookie.parse(url, cookieString.trim()) ?: continue
                cookies.add(cookie)
            }
        }
        if (url.host.contains("reddit.com")) {
            cookies.add(OVER_18)
        }
        // Redlib instances are hosted on `redlib.*` hosts. Only add the cookie
        // when the user has asked for NSFW content; Redlib defaults to off.
        if (url.host.contains("redlib") && showNsfw()) {
            // Domain is the request host itself (host-bound cookie). OkHttp's
            // Cookie.Builder.build() requires a non-null domain, so it cannot
            // be left unset as it was in the first attempt.
            cookies.add(
                Cookie.Builder()
                    .name("show_nsfw")
                    .value("on")
                    .domain(url.host)
                    .path("/")
                    .secure()
                    .build()
            )
        }
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookieManager = CookieManager.getInstance()
        for (cookie in cookies) {
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        cookieManager.flush()
    }

    companion object {
        private val OVER_18 = Cookie.Builder()
            .name("over18")
            .value("1")
            .domain("reddit.com")
            .path("/")
            .secure()
            .build()
    }
}
