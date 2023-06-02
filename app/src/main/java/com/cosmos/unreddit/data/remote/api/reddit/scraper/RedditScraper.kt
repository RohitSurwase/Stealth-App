package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.Awarding
import com.cosmos.unreddit.data.remote.api.reddit.model.RichText
import com.cosmos.unreddit.data.remote.api.reddit.model.Tagline
import com.cosmos.unreddit.data.remote.scraper.Scraper
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.extension.toSeconds
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element

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

    protected fun Element.getTagline(): Tagline {
        val tagline = selectFirst("p.tagline")

        val created = tagline
            ?.selectFirst("time.live-timestamp")
            ?.toTimeInSeconds()
            ?: System.currentTimeMillis().toSeconds()

        val edited = tagline
            ?.selectFirst("time.edited-timestamp")
            ?.toTimeInSeconds()
            ?: -1

        val awardings = tagline
            ?.selectFirst("span.awardings-bar")
        val awards = awardings
            ?.select("a.awarding-link")
            ?.map { award -> award.toAwarding() }
            ?: emptyList()

        val moreAwards = awardings
            ?.selectFirst("a.awarding-show-more-link")
            ?.text()
            ?.run { MORE_AWARDS_REGEX.find(this)?.value?.toIntOrNull() }
            ?: 0

        // total_awards_received
        val totalAwards = awards.sumOf { it.count }.plus(moreAwards)

        // link_flair_richtext
        val flairRichText = tagline
            ?.selectFirst("span.flairrichtext")
            ?.toFlair()
            ?: emptyList()

        val score = tagline
            ?.selectFirst("span.score.unvoted")
            ?.attr("title")
            ?.toIntOrNull()
            ?: 0

        val scoreHidden = tagline?.selectFirst("span.score-hidden") != null

        val isStickied = tagline?.selectFirst("span.stickied-tagline") != null

        // author
        val authorClass = tagline?.selectFirst("a.author")
        val author = authorClass?.text() ?: "[deleted]"

        // distinguished
        val distinguished = when {
            authorClass == null -> "regular"
            authorClass.hasClass("moderator") -> "moderator"
            authorClass.hasClass("admin") -> "admin"
            else -> "regular"
        }

        val isSubmitter = authorClass?.hasClass("submitter") ?: false

        return Tagline(
            created,
            edited,
            author,
            distinguished,
            score,
            scoreHidden,
            isSubmitter,
            isStickied,
            awards,
            totalAwards,
            flairRichText
        )
    }

    private fun Element.toFlair(): List<RichText> {
        return children().map { flair ->
            when {
                flair.hasClass("flairemoji") -> {
                    val style = flair.attr("style")
                    val url = URL_REGEX.find(style)?.groups?.get(1)?.value
                    RichText(null, url)
                }

                else -> {
                    RichText(flair.text(), null)
                }
            }
        }
    }

    private fun Element.toAwarding(): Awarding {
        val count = attr("data-count").toIntOrNull() ?: 0
        val url = selectFirst("img")?.attr("src").orEmpty()

        return Awarding(url, emptyList(), count)
    }

    private fun Element.toTimeInSeconds(): Long {
        val datetime = attr("datetime")
        val time = DateUtil.getDateFromString("yyyy-MM-dd'T'HH:mm:ss", datetime)?.time
            ?: System.currentTimeMillis()

        return time.toSeconds()
    }

    companion object {
        private val MORE_AWARDS_REGEX = Regex("\\d+")
        private val URL_REGEX = Regex("url\\((.*?)\\)")
    }
}
