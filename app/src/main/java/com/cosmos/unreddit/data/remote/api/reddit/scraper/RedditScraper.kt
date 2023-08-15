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
    ioDispatcher: CoroutineDispatcher
) : Scraper<Result>(ioDispatcher) {

    protected fun getNextKey(): String? {
        return document?.selectFirst("span.next-button")
            ?.selectFirst("a[href]")
            ?.attr(Scraper.Selector.Attr.HREF)
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
            ?.toRichFlairText()
            ?: emptyList()

        val flair = tagline
            ?.selectFirst("span.flair")
            ?.text()

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
            flairRichText,
            flair
        )
    }

    protected fun Element.toRichFlairText(): List<RichText> {
        val flairRichText = selectFirst(Selector.FLAIR_RICH_TEXT)

        return flairRichText?.children()?.map { flair ->
            when {
                flair.hasClass("flairemoji") -> {
                    val style = flair.attr("style")
                    val url = URL_REGEX.find(style)?.groups?.get(1)?.value
                    RichText(null, url)
                }

                else -> {
                    RichText(flair.wholeText(), null)
                }
            }
        } ?: emptyList()
    }

    private fun Element.toAwarding(): Awarding {
        val count = attr(Selector.Attr.COUNT).toIntOrNull() ?: 0
        val url = selectFirst(Scraper.Selector.Tag.IMG)?.attr(Scraper.Selector.Attr.SRC).orEmpty()

        return Awarding(url, emptyList(), count)
    }

    protected fun Element.toTimeInSeconds(): Long {
        val datetime = attr("datetime")
        val time = DateUtil.getDateFromString(DATETIME_FORMAT, datetime)?.time
            ?: System.currentTimeMillis()

        return time.toSeconds()
    }

    protected fun Element.toInt(): Int? {
        return text()
            .run { replace(",", "") }
            .run { toIntOrNull() }
    }

    protected fun String.toValidLink(): String {
        return "https:$this"
    }
    
    protected object Selector {
        object Class {
            const val THING = "thing"
            const val MORE_CHILDREN = "morechildren"
            const val MORE_RECURSION = "morerecursion"
            const val CONTROVERSIAL = "controversial"
            const val SELF = "self"
            const val LOCKED = "locked"
            const val STICKIED = "stickied"
        }

        object Attr {
            const val FULLNAME = "data-fullname"
            const val PERMALINK = "data-permalink"
            const val SUBREDDIT = "data-subreddit"
            const val SUBREDDIT_PREFIXED = "data-subreddit-prefixed"
            const val PROMOTED = "data-promoted"
            const val OC = "data-oc"
            const val SCORE = "data-score"
            const val DOMAIN = "data-domain"
            const val NSFW = "data-nsfw"
            const val SPOILER = "data-spoiler"
            const val COMMENT_COUNT = "data-comments-count"
            const val URL = "data-url"
            const val TIMESTAMP = "data-timestamp"
            const val IS_GALLERY = "data-is-gallery"
            const val CACHED_HTML = "data-cachedhtml"
            const val MEDIA_ID = "data-media-id"
            const val CROSSPOST_ROOT_TITLE = "data-crosspost-root-title"
            const val CROSSPOST_ROOT_AUTHOR = "data-crosspost-root-author"
            const val CROSSPOST_ROOT_SUBREDDIT_PREFIXED = "data-crosspost-root-subreddit-prefixed"
            const val CROSSPOST_ROOT_TIME = "data-crosspost-root-time"
            const val SR_NAME = "data-sr_name"
            const val COUNT = "data-count"
        }
        
        const val POST = "div[id~=thing_t3_\\w*]"

        const val MD = "div.md"

        const val SITETABLE = "div.sitetable"

        const val SITETABLE_NESTED = "div.sitetable.nestedlisting"
        const val SITETABLE_LINK = "div.sitetable.linklisting"

        const val FLAIR_RICH_TEXT = "span.flairrichtext"

        const val NUMBER = "span.number"
    }

    companion object {
        private val MORE_AWARDS_REGEX = Regex("\\d+")
        private val URL_REGEX = Regex("url\\((.*?)\\)")

        private const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    }
}
