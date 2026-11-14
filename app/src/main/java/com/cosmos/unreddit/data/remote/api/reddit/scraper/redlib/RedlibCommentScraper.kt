package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentData
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Scrapes the comment tree of a Redlib post page.
 *
 * Top-level comments are wrapped in `div.thread`; replies are direct children
 * of the parent's `blockquote.replies`. Redlib has no `/api/morechildren`
 * equivalent, so "More replies" links are intentionally ignored.
 */
class RedlibCommentScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String,
    private val linkId: String,
    private val subreddit: String
) : RedlibScraper<Listing>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Listing {
        val children = document.select("div.thread")
            .mapNotNull { thread ->
                thread.children()
                    .firstOrNull { it.hasClass("comment") }
                    ?.toComment(0)
            }

        return Listing(
            KIND,
            ListingData(
                null,
                null,
                children,
                null,
                null
            )
        )
    }

    private fun Element.toComment(depth: Int): Child {
        val id = id()

        val authorElement = selectFirst("a.comment_author")
            ?: selectFirst("span.comment_author")
        val author = authorElement?.text()?.removePrefix("u/") ?: "[deleted]"

        val scoreElement = selectFirst("p.comment_score")
            ?: selectFirst("span.comment_score")
        val scoreTitle = scoreElement?.attr("title").orEmpty()
        val scoreHidden = scoreTitle.isBlank() ||
            scoreTitle.equals("Hidden", ignoreCase = true)
        val score = scoreTitle.toScore()

        val createdElement = selectFirst("a.created")
        val created = createdElement?.timestamp() ?: nowInSeconds()
        val edited = selectFirst("span.edited")
            ?.timestamp()
            ?: -1L

        val bodyElement = selectFirst("div.comment_body")
        val bodyHtml = bodyElement?.outerHtml().orEmpty()

        val permalink = createdElement
            ?.attr("href")
            ?.substringBefore("?")
            ?.substringBefore("#")
            .orEmpty()

        val replies = selectFirst("blockquote.replies")
            ?.children()
            ?.asSequence()
            ?.filter { it.hasClass("comment") }
            ?.map { it.toComment(depth + 1) }
            ?.toList()
            .orEmpty()
            .let { children ->
                if (children.isEmpty()) {
                    null
                } else {
                    Listing(KIND, ListingData(null, null, children, null, null))
                }
            }

        val distinguished = when {
            authorElement == null -> null
            authorElement.hasClass("moderator") -> "moderator"
            authorElement.hasClass("admin") -> "admin"
            else -> null
        }

        val data = CommentData(
            totalAwards = select("span.award").size,
            authorFlairRichText = null,
            linkId = linkId,
            replies = replies,
            author = author,
            score = score,
            awardings = emptyList(),
            bodyHtml = bodyHtml,
            edited = edited,
            isSubmitter = authorElement?.hasClass("op") == true,
            stickied = false,
            scoreHidden = scoreHidden,
            permalink = permalink,
            id = id,
            name = "t1_$id",
            created = created,
            controversiality = 0,
            flair = null,
            depth = depth,
            distinguished = distinguished,
            subreddit = subreddit,
            linkTitle = null,
            linkPermalink = null,
            linkAuthor = null
        )

        return CommentChild(data)
    }

    companion object {
        private const val KIND = "t1"
    }
}
