package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class SubredditSearchScraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Listing>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): Listing {
        val subreddits = document.select("div.search-result-subreddit")

        val children = subreddits.map { it.toSubreddit() }
        val after = getNextKey()

        return Listing(
            KIND,
            ListingData(
                null,
                null,
                children,
                after,
                null
            )
        )
    }

    private fun Element.toSubreddit(): AboutChild {
        val subscribeButton = selectFirst("span.search-subscribe-button")
        val name = subscribeButton?.attr("data-sr_name").orEmpty()

        val title = selectFirst("a.search-title")?.text().orEmpty()

        val over18 = selectFirst("span.stamp nsfw-stamp") != null

        val link = selectFirst("a.search-subreddit-link")?.attr("href").orEmpty()

        val subscribers = selectFirst("span.search-subscribers")
            ?.text()
            ?.run { SUBSCRIBERS_REGEX.find(this)?.value }
            ?.run { replace(",", "") }
            ?.run { toIntOrNull() }

        val data = AboutData(
            null,
            name,
            null,
            title,
            null,
            null,
            null,
            null,
            subscribers,
            null,
            null,
            "",
            "",
            null,
            null,
            over18,
            null,
            link,
            0L // TODO
        )

        return AboutChild(data)
    }

    companion object {
        private const val KIND = "t5"

        private val SUBSCRIBERS_REGEX = Regex("(\\d+,?)+")
    }
}
