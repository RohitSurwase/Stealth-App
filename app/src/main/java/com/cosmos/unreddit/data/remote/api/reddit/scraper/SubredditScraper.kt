package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.scraper.Scraper
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

class SubredditScraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Child>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): Child {
        val title = document.selectFirst(Scraper.Selector.Tag.TITLE)?.text().orEmpty()

        val redditName = document.selectFirst("h1.redditname")
            ?.selectFirst(Scraper.Selector.Tag.A)

        val name = redditName?.text().orEmpty()
        val link = redditName?.attr(Scraper.Selector.Attr.HREF).orEmpty()

        val communityIcon = document.selectFirst("img[id=header-img]")
            ?.attr(Scraper.Selector.Attr.SRC)
            ?.toValidLink()
            .orEmpty()

        val subscribers = document.selectFirst("span.subscribers")
            ?.selectFirst(Selector.NUMBER)
            ?.toInt()

        val activeUsers = document.selectFirst("p.users-online")
            ?.selectFirst(Selector.NUMBER)
            ?.toInt()

        val descriptionHtml = document.selectFirst("div.titlebox")
            ?.selectFirst(Selector.MD)
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
