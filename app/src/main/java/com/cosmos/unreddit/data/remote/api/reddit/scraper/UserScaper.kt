package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

class UserScaper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Child>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): Child {
        val name = document.selectFirst("div.titlebox")
            ?.selectFirst("h1")
            ?.text()
            .orEmpty()

        val postKarma = document.selectFirst("span.karma")
            ?.text()
            ?.run { replace(",", "") }
            ?.run { toIntOrNull() }
            ?: 0

        val commentKarma = document.selectFirst("span.karma.comment-karma")
            ?.text()
            ?.run { replace(",", "") }
            ?.run { toIntOrNull() }
            ?: 0

        val created = document.selectFirst("span.age")
            ?.selectFirst("time")
            ?.toTimeInSeconds()
            ?: 0L

        val data = AboutUserData(
            isSuspended = false,
            isEmployee = false,
            null,
            null,
            null,
            postKarma,
            -1,
            name,
            created,
            null,
            commentKarma
        )

        return AboutUserChild(data)
    }
}
