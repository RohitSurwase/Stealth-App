package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.scraper.Scraper
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

abstract class RedditScraper<Result>(
    body: String,
    ioDispatcher: CoroutineDispatcher
) : Scraper<Result>(body, ioDispatcher) {

    protected fun getNextKey(): String? {
        return document?.selectFirst("span.next-button")
            ?.selectFirst("a[href]")
            ?.attr("href")
            ?.toHttpUrlOrNull()
            ?.queryParameter("after")
    }
}
