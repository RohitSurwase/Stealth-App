package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class RedditCookieJar : CookieJar {

    private val cookies = mutableSetOf<Cookie>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.toList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies
            .find { it.name == "over18" }
            ?.let { this.cookies.add(it) }
    }
}
