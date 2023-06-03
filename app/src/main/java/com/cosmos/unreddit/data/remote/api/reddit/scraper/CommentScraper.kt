package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentData
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChild
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreData
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class CommentScraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Listing>(ioDispatcher) {

    private var linkId: String = ""

    override suspend fun scrapDocument(document: Document): Listing {
        val post = document.selectFirst("div[id~=thing_t3_\\w*]")
            ?.attr("data-fullname")

        linkId = post.orEmpty()

        val after = getNextKey()

        val siteTable = document.selectFirst("div.sitetable.nestedlisting")
            ?: document.selectFirst("div.sitetable.linklisting")

        return siteTable
            ?.run { getComments(0) }
            .run {
                Listing(
                    KIND,
                    ListingData(
                        null,
                        null,
                        this ?: emptyList(),
                        after,
                        null
                    )
                )
            }
    }

    private fun Element.getComments(depth: Int, parentName: String? = null): List<Child> {
        return children()
            .filter { element -> element.hasClass("thing") }
            .mapNotNull { comment ->
                when {
                    comment.hasClass("morechildren") -> comment.toMore(depth, parentName)
                    !comment.hasClass("morerecursion") -> comment.toComment(depth)
                    else -> null
                }
            }
    }

    private fun Element.toComment(depth: Int): CommentChild {
        val entry = selectFirst("div.entry")

        val bodyHtml = entry?.selectFirst("div.md")?.outerHtml().orEmpty()

        val name = attr("data-fullname")

        val parent = selectFirst("p.parent")
        val parentTitle = parent?.selectFirst("a.title")
        val linkTitle = parentTitle?.text()
        val linkPermalink = parentTitle?.attr("href")
        val linkAuthor = parent?.selectFirst("a.author")?.text()

        val children = selectFirst("div.sitetable")
        val comments = children
            ?.run { this.getComments(depth + 1, name) }
            ?.run { Listing(KIND, ListingData(null, null, this, null, null)) }

        val permalink = attr("data-permalink")

        val controversiality = if (hasClass("controversial")) 1 else 0

        val subreddit = attr("data-subreddit")

        val tagline = getTagline()

        val data = CommentData(
            tagline.totalAwards,
            tagline.flairRichText,
            linkId,
            comments,
            tagline.author,
            tagline.score,
            tagline.awardings,
            bodyHtml,
            tagline.edited,
            tagline.isSubmitter,
            tagline.stickied,
            tagline.scoreHidden,
            permalink,
            name,
            name,
            tagline.created,
            controversiality,
            tagline.flair,
            depth,
            tagline.distinguished,
            subreddit,
            linkTitle,
            linkPermalink,
            linkAuthor
        )

        return CommentChild(data)
    }

    private fun Element.toMore(depth: Int, parentName: String?): MoreChild {
        val moreComments = selectFirst("span.morecomments")
            ?.selectFirst("a")
            ?.attr("onclick")
            ?.run { MORE_REGEX.find(this)?.groups?.get(1)?.value }
            ?.run { split(',') }
            ?: emptyList()

        val name = attr("data-fullname")

        val data = MoreData(
            moreComments.size,
            name,
            name,
            depth,
            parentName.orEmpty(),
            moreComments.toMutableList()
        )

        return MoreChild(data)
    }

    companion object {
        private const val KIND = "t1"

        private val MORE_REGEX = Regex("'\\w+:(.*?)'")
    }
}
