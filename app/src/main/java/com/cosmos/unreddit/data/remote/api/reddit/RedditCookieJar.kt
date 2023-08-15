package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class RedditCookieJar : CookieJar {

    private val cookies = listOf(OVER_18)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // ignore
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
