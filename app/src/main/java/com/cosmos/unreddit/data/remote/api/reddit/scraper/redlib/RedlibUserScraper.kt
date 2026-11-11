package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

/**
 * Scrapes the user panel of a Redlib profile page.
 */
class RedlibUserScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String
) : RedlibScraper<Child>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Child {
        val name = document.selectFirst("#user_name")
            ?.text()
            ?.removePrefix("u/")
            ?: ""

        val icon = document.selectFirst("#user_icon")?.attr("src")?.toAbsoluteUrl()

        val details = document.select("#user_details > div")
        val karma = details.getOrNull(0)
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toScore()
            ?: 0
        val createdText = details.getOrNull(1)?.text()

        val data = AboutUserData(
            isSuspended = false,
            isEmployee = false,
            subreddit = null,
            id = null,
            iconImg = icon,
            linkKarma = 0,
            totalKarma = karma,
            name = name,
            created = parseTimestamp(createdText),
            snoovatarImg = null,
            commentKarma = 0
        )

        return AboutUserChild(data)
    }
}
