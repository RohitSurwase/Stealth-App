package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

class SubredditScraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Child>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): Child {
        val title = document.selectFirst("title")?.text().orEmpty()

        val redditName = document.selectFirst("h1.redditname")
            ?.selectFirst("a")

        val name = redditName?.text().orEmpty()
        val link = redditName?.attr("href").orEmpty()

        val communityIcon = document.selectFirst("img[id=header-img]")
            ?.attr("src")
            ?.run { "https:$this" }
            .orEmpty()

        val subscribers = document.selectFirst("span.subscribers")
            ?.selectFirst("span.number")
            ?.text()
            ?.run { replace(",", "") }
            ?.run { toIntOrNull() }

        val activeUsers = document.selectFirst("p.users-online")
            ?.selectFirst("span.number")
            ?.text()
            ?.run { replace(",", "") }
            ?.run { toIntOrNull() }

        val descriptionHtml = document.selectFirst("div.titlebox")
            ?.selectFirst("div.md")
            ?.outerHtml()

        val data = AboutData(
            null,
            name,
            null,
            title,
            null,
            activeUsers,
            null,
            subscribers,
            null,
            null,
            communityIcon,
            "",
            null,
            null,
            false,
            descriptionHtml,
            link,
            0L // TODO
        )

        return AboutChild(data)
    }
}
