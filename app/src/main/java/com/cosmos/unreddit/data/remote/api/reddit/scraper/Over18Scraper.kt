package com.cosmos.unreddit.data.remote.api.reddit.scraper

import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document

class Over18Scraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<String?>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): String? {
        val over18Interstitial = document.select("button[type=submit][name=over18]")

        if (over18Interstitial.isEmpty()) return null

        val rel = document.selectFirst("link[rel=canonical]")
            ?.attr("href")

        return rel?.toHttpUrlOrNull()?.queryParameter("dest").orEmpty()
    }
}
