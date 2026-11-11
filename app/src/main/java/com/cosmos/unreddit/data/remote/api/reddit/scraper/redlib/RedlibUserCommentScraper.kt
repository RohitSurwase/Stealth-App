package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentData
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document

/**
 * Scrapes the comment list on a Redlib user profile (`/user/:name/comments`).
 *
 * These entries are not a comment tree: Redlib renders each comment as a
 * standalone `div.comment.user-comment` with the post title in the summary.
 */
class RedlibUserCommentScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String,
    private val user: String
) : RedlibScraper<Listing>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Listing {
        val children = document.select("div.comment.user-comment")
            .map { it.toComment() }

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

    private fun org.jsoup.nodes.Element.toComment(): Child {
        val linkElement = selectFirst("a.comment_link")
        val linkPermalink = linkElement?.attr("href").orEmpty()
        val linkTitle = linkElement?.text()

        val permalink = linkPermalink.substringBefore("#").substringBefore("?")
        val id = COMMENT_ID_REGEX.find(linkPermalink)?.groupValues?.getOrNull(1)
            ?: permalink.hashCode().toString()

        val scoreTitle = selectFirst("p.comment_score")
            ?.attr("title")
            .orEmpty()
        val scoreHidden = scoreTitle.isBlank() ||
            scoreTitle.equals("Hidden", ignoreCase = true)

        val subreddit = selectFirst("a.comment_subreddit")
            ?.text()
            ?.removePrefix("r/")
            ?: permalink.substringAfter("/r/").substringBefore("/")

        val bodyHtml = selectFirst(".comment_body")?.outerHtml().orEmpty()

        val data = CommentData(
            totalAwards = 0,
            authorFlairRichText = null,
            linkId = "",
            replies = null,
            author = user,
            score = scoreTitle.toScore(),
            awardings = emptyList(),
            bodyHtml = bodyHtml,
            edited = -1L,
            isSubmitter = false,
            stickied = false,
            scoreHidden = scoreHidden,
            permalink = permalink,
            id = id,
            name = "t1_$id",
            created = selectFirst("span.created")?.timestamp() ?: nowInSeconds(),
            controversiality = 0,
            flair = null,
            depth = 0,
            distinguished = null,
            subreddit = subreddit,
            linkTitle = linkTitle,
            linkPermalink = permalink,
            linkAuthor = null
        )

        return CommentChild(data)
    }

    companion object {
        private const val KIND = "t1"

        private val COMMENT_ID_REGEX = Regex("/comments/[^/]+/[^/]+/([^/?#]+)")
    }
}
