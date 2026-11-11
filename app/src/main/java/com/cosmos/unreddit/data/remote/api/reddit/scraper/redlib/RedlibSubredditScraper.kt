package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

/**
 * Scrapes the subreddit panel of a Redlib community page.
 */
class RedlibSubredditScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String
) : RedlibScraper<Child>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Child {
        val name = document.selectFirst("#sub_name")
            ?.text()
            ?.removePrefix("r/")
            ?: document.selectFirst(SUBREDDIT_LINK)
                ?.attr("href")
                ?.removePrefix("/r/")
                .orEmpty()

        val title = document.selectFirst("#sub_title")?.text().orEmpty()
        val icon = document.selectFirst("#sub_icon")?.attr("src")?.toAbsoluteUrl().orEmpty()

        val details = document.select("#sub_details > div")
        val subscribers = details.getOrNull(0)
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toScore()
            ?: 0
        val activeUsers = details.getOrNull(1)
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toScore()

        val description = document.selectFirst("#sub_description")?.text()
        val descriptionHtml = document.selectFirst("#sidebar_contents")?.outerHtml()
            ?: document.selectFirst("#sub_description")?.outerHtml()

        val data = AboutData(
            wikiEnabled = null,
            displayName = name,
            headerImg = null,
            title = title.ifBlank { name },
            primaryColor = null,
            activeUserCount = activeUsers,
            iconImg = icon,
            subscribers = subscribers,
            quarantine = null,
            publicDescriptionHtml = descriptionHtml,
            communityIcon = icon,
            bannerBackgroundImage = "",
            keyColor = null,
            bannerBackgroundColor = null,
            over18 = false,
            descriptionHtml = descriptionHtml ?: description,
            url = "/r/$name".toAbsoluteUrl(),
            created = 0L
        )

        return AboutChild(data)
    }

    companion object {
        private const val SUBREDDIT_LINK = "a.post_subreddit"
    }
}
