package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Scrapes subreddit results from a Redlib search page (`?type=sr_user`).
 */
class RedlibSubredditSearchScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String
) : RedlibScraper<Listing>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Listing {
        val children = document.select("a.search_subreddit[href^=\"/r/\"]")
            .map { it.toSubreddit() }

        return Listing(
            KIND,
            ListingData(
                null,
                null,
                children,
                getNextKey(),
                null
            )
        )
    }

    private fun Element.toSubreddit(): Child {
        val name = selectFirst(".search_subreddit_name")
            ?.text()
            ?.removePrefix("r/")
            ?: attr("href").removePrefix("/r/").trimEnd('/')

        val subscribers = selectFirst(".search_subreddit_members")
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toScore()
            ?: 0

        val icon = selectFirst("img")
            ?.attr("src")
            ?.toAbsoluteUrl()
            .orEmpty()

        val description = selectFirst(".search_subreddit_description")?.text()

        val data = AboutData(
            wikiEnabled = null,
            displayName = name,
            headerImg = null,
            title = name,
            primaryColor = null,
            activeUserCount = null,
            iconImg = icon,
            subscribers = subscribers,
            quarantine = null,
            publicDescriptionHtml = description,
            communityIcon = icon,
            bannerBackgroundImage = "",
            keyColor = null,
            bannerBackgroundColor = null,
            over18 = false,
            descriptionHtml = null,
            url = "/r/$name".toAbsoluteUrl(),
            created = 0L
        )

        return AboutChild(data)
    }

    companion object {
        private const val KIND = "t5"
    }
}
