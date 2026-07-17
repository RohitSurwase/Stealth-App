package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import android.webkit.CookieManager

class RedditCookieJar : CookieJar {

    private val cookies = listOf(OVER_18)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieManager = CookieManager.getInstance()
        val cookies = mutableListOf<Cookie>()
        val savedCookies = cookieManager.getCookie(url.toString())
        if (savedCookies != null) {
            val splitCookies = savedCookies.split("[,;]".toRegex())
            for (cookieString in splitCookies) {
                cookies.add(Cookie.parse(url, cookieString.trim())!!)
            }
        }
        if (url.host.contains("reddit.com")) {
            cookies.add(OVER_18)
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
