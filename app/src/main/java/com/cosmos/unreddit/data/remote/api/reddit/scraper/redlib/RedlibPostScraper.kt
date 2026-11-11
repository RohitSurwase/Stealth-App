package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.GalleryData
import com.cosmos.unreddit.data.remote.api.reddit.model.GalleryDataItem
import com.cosmos.unreddit.data.remote.api.reddit.model.GalleryImage
import com.cosmos.unreddit.data.remote.api.reddit.model.GalleryItem
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import com.cosmos.unreddit.data.remote.api.reddit.model.Media
import com.cosmos.unreddit.data.remote.api.reddit.model.MediaMetadata
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.reddit.model.PostData
import com.cosmos.unreddit.data.remote.api.reddit.model.RedditVideoPreview
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Scrapes a Redlib post listing, and (when the page contains a single post)
 * a post detail page. Both use the same CSS in Redlib's templates, the only
 * difference being the heading level and the presence of an `id` attribute.
 */
class RedlibPostScraper(
    ioDispatcher: CoroutineDispatcher,
    baseUrl: String
) : RedlibScraper<Listing>(ioDispatcher, baseUrl) {

    override suspend fun scrapDocument(document: Document): Listing {
        val posts = document.select(Selector.POST_WITH_ID)
            .ifEmpty { listOfNotNull(document.selectFirst(Selector.POST)) }

        val children = posts.mapNotNull { it.toPost() }
        val after = getNextKey()

        return Listing(
            KIND,
            ListingData(
                null,
                null,
                children,
                after,
                null
            )
        )
    }

    private fun Element.toPost(): Child? {
        val permalink = getPermalink() ?: return null

        val subreddit = selectFirst(Selector.POST_SUBREDDIT)
            ?.text()
            ?.removePrefix("r/")
            ?: permalink.substringAfter("/r/").substringBefore("/")

        val authorElement = selectFirst(Selector.POST_AUTHOR)
        val author = authorElement?.text()?.removePrefix("u/") ?: "[deleted]"

        val title = selectFirst(Selector.POST_TITLE_LINK)?.text()
            ?: selectFirst(Selector.POST_TITLE)?.titleText().orEmpty()

        val flair = selectFirst(Selector.POST_FLAIR)?.text()

        val score = selectFirst(Selector.POST_SCORE)
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toScore()
            ?: 0

        val commentsNumber = selectFirst(Selector.POST_COMMENTS)
            ?.let { it.attr("title").ifBlank { it.text() } }
            ?.toCommentCount()
            ?: document?.selectFirst(Selector.COMMENT_COUNT)
                ?.text()
                ?.toCommentCount()
            ?: 0

        val created = selectFirst(Selector.CREATED)?.timestamp() ?: nowInSeconds()

        val mediaContent = selectFirst(Selector.MEDIA_CONTENT)
        val video = mediaContent?.selectFirst("video")
        val imageAnchor = mediaContent?.selectFirst("a.post_media_image")
        val imageUrl = imageAnchor?.attr("href")?.toAbsoluteUrl()
            ?: imageAnchor?.selectFirst("img[src]")?.attr("src")?.toAbsoluteUrl()
            ?: mediaContent?.selectFirst("img[src]")?.attr("src")?.toAbsoluteUrl()

        val galleryUrls = selectFirst("div.gallery")
            ?.select("figure")
            ?.mapNotNull { figure ->
                figure.selectFirst("a[href]")?.attr("href")?.toAbsoluteUrl()
                    ?: figure.selectFirst("img[src]")?.attr("src")?.toAbsoluteUrl()
            }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val videoUrl = video?.attr("src")?.toAbsoluteUrl()
        val posterUrl = video?.attr("poster")?.toAbsoluteUrl()

        val thumbnailElement = selectFirst(Selector.POST_THUMBNAIL)
        val thumbnailUrl = thumbnailElement
            ?.selectFirst("svg image[href]")
            ?.attr("href")
            ?.toAbsoluteUrl()
            ?: thumbnailElement
                ?.selectFirst("img[src]")
                ?.attr("src")
                ?.toAbsoluteUrl()

        val externalUrl = selectFirst("a#post_url")?.attr("href")?.toAbsoluteUrl()
            ?: thumbnailElement
                ?.attr("href")
                ?.takeIf { it.startsWith("http", ignoreCase = true) && !it.contains("/comments/") }

        val isGallery = galleryUrls.isNotEmpty()
        val isVideo = videoUrl != null
        val isImage = !isGallery && !isVideo && imageUrl != null
        val isLink = !isGallery && !isVideo && !isImage && externalUrl != null

        val url = when {
            isGallery -> galleryUrls.first()
            isVideo -> videoUrl.orEmpty()
            isImage -> imageUrl.orEmpty()
            isLink -> externalUrl.orEmpty()
            else -> permalink.toAbsoluteUrl()
        }

        val isSelf = !isGallery && !isVideo && !isImage && !isLink

        val domain = url.toHttpUrlOrNull()?.host.orEmpty()

        val galleryData = if (isGallery) {
            GalleryData(
                galleryUrls.mapIndexed { index, _ ->
                    GalleryDataItem(null, index.toString())
                }
            )
        } else {
            null
        }

        val mediaMetadata = if (isGallery) {
            MediaMetadata(
                galleryUrls.mapIndexed { index, galleryUrl ->
                    val isGalleryVideo = galleryUrl.substringBefore('?').let {
                        it.endsWith(".mp4") || it.endsWith(".webm")
                    }
                    GalleryItem(
                        null,
                        GalleryImage(
                            0,
                            0,
                            if (isGalleryVideo) null else galleryUrl,
                            if (isGalleryVideo) galleryUrl else null
                        ),
                        null,
                        index.toString()
                    )
                }
            )
        } else {
            null
        }

        val media = if (isVideo) {
            Media(
                null,
                null,
                RedditVideoPreview(videoUrl.orEmpty(), 0, 0, 0, false)
            )
        } else {
            null
        }

        val selfTextHtml = selectFirst("div.post_body")?.outerHtml()

        val distinguished = when {
            authorElement == null -> null
            authorElement.hasClass("moderator") -> "moderator"
            authorElement.hasClass("admin") -> "admin"
            else -> null
        }

        val id = id().ifBlank { permalink.postId() }
        val name = "t3_$id"

        val postData = PostData(
            subreddit = subreddit,
            linkFlairRichText = emptyList(),
            authorFlairRichText = null,
            title = title,
            prefixedSubreddit = "r/$subreddit",
            name = name,
            ratio = selectFirst("div.post_footer p")?.text()?.substringBefore('%')?.trim()?.toIntOrNull()
                ?.let { it / 100.0 },
            totalAwards = select(Selector.AWARD).size,
            isOC = false,
            flair = flair,
            authorFlair = null,
            galleryData = galleryData,
            score = score,
            hint = if (isImage || isGallery) "image" else null,
            isSelf = isSelf,
            crossposts = null,
            domain = if (isSelf) "" else domain,
            selfTextHtml = selfTextHtml,
            suggestedSort = null,
            isArchived = false,
            isOver18 = selectFirst(Selector.NSFW) != null,
            mediaPreview = null,
            awardings = emptyList(),
            isSpoiler = selectFirst(Selector.SPOILER) != null,
            isLocked = false,
            distinguished = distinguished,
            author = author,
            commentsNumber = commentsNumber,
            permalink = permalink,
            isStickied = hasClass("stickied"),
            url = url,
            created = created,
            media = media,
            mediaMetadata = mediaMetadata,
            isRedditGallery = isGallery,
            isVideo = isVideo
        ).apply {
            this.thumbnail = posterUrl ?: thumbnailUrl ?: if (isImage) imageUrl else null
        }

        return PostChild(postData)
    }

    private fun Element.getPermalink(): String? {
        return selectFirst(Selector.POST_TITLE_LINK)?.attr("href")
            ?: selectFirst("div.post_footer a[href*=\"/comments/\"]")?.attr("href")
            ?: document?.selectFirst("meta[property=og:url]")?.attr("content")
    }

    private fun Element.titleText(): String {
        val clone = clone()
        clone.select("a.post_flair, small.nsfw, small.spoiler").remove()
        return clone.text().trim()
    }

    private fun String.postId(): String {
        return POST_ID_REGEX.find(this)?.groupValues?.getOrNull(1)
            ?: hashCode().toString()
    }

    companion object {
        private const val KIND = "t3"

        private val POST_ID_REGEX = Regex("/comments/([^/?#]+)")

        private object Selector {
            const val POST = "div.post"
            const val POST_WITH_ID = "div.post[id]"
            const val POST_SUBREDDIT = "p.post_header a.post_subreddit"
            const val POST_AUTHOR = "p.post_header a.post_author"
            const val POST_TITLE = "h1.post_title, h2.post_title"
            const val POST_TITLE_LINK = "h2.post_title a:not(.post_flair)"
            const val POST_FLAIR = "a.post_flair"
            const val POST_SCORE = "div.post_score"
            const val POST_COMMENTS = "a.post_comments"
            const val POST_THUMBNAIL = "a.post_thumbnail"
            const val MEDIA_CONTENT = "div.post_media_content"
            const val CREATED = "p.post_header .created"
            const val COMMENT_COUNT = "#comment_count"
            const val AWARD = "span.award"
            const val NSFW = "small.nsfw"
            const val SPOILER = "small.spoiler"
        }
    }
}
