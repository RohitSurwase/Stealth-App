package com.cosmos.unreddit.data.remote.api.reddit.scraper

import com.cosmos.unreddit.data.remote.api.reddit.model.Crosspost
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
import com.cosmos.unreddit.data.remote.scraper.Scraper
import com.cosmos.unreddit.util.extension.toSeconds
import kotlinx.coroutines.CoroutineDispatcher
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class PostScraper(
    ioDispatcher: CoroutineDispatcher
) : RedditScraper<Listing>(ioDispatcher) {

    override suspend fun scrapDocument(document: Document): Listing {
        val posts = document.select(Selector.POST)
            .filter { element -> !element.attr(Selector.Attr.PROMOTED).toBoolean() }

        val children = posts.map { it.toPost() }
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

    private fun Element.toPost(): PostChild {
        // subreddit
        val subreddit = attr(Selector.Attr.SUBREDDIT)

        val titleParagraph = selectFirst("p.title")
        // title
        val title = titleParagraph?.selectFirst(Scraper.Selector.Tag.A)?.text().orEmpty()

        // link_flair_richtext
        val flairRichText = titleParagraph
            ?.toRichFlairText()
            ?: emptyList()

        val flair = titleParagraph
            ?.selectFirst("span.linkflairlabel")
            ?.text()

        // subreddit_name_prefixed
        val prefixedSubreddit = attr(Selector.Attr.SUBREDDIT_PREFIXED)

        // name
        val name = attr(Selector.Attr.FULLNAME)

        // is_original_content
        val isOC = attr(Selector.Attr.OC).toBoolean()

        // score
        val score = attr(Selector.Attr.SCORE).toIntOrNull() ?: 0

        // domain
        val domain = attr(Selector.Attr.DOMAIN)

        // is_self
        val isSelf = hasClass(Selector.Class.SELF)

        // crosspost_parent_list
        val crosspostTitle = attr(Selector.Attr.CROSSPOST_ROOT_TITLE)
        val crosspostAuthor = attr(Selector.Attr.CROSSPOST_ROOT_AUTHOR)
        val crosspostSubredditPrefixed = attr(Selector.Attr.CROSSPOST_ROOT_SUBREDDIT_PREFIXED)
        val crosspostTime = attr(Selector.Attr.CROSSPOST_ROOT_TIME) // TODO: format = 1 year ago

        val crosspost = crosspostAuthor
            .takeIf { it.isNotBlank() }
            ?.run {
                Crosspost(crosspostAuthor, crosspostSubredditPrefixed, crosspostTitle, null)
            }

        // over_18
        val isOver18 = attr(Selector.Attr.NSFW).toBoolean()

        // spoiler
        val isSpoiler = attr(Selector.Attr.SPOILER).toBoolean()

        // locked
        val isLocked = hasClass(Selector.Class.LOCKED)

        // num_comments
        val commentsNumber = attr(Selector.Attr.COMMENT_COUNT).toIntOrNull() ?: 0

        // permalink
        val permalink = attr(Selector.Attr.PERMALINK)

        // stickied
        val isStickied = hasClass(Selector.Class.STICKIED)

        // url
        val url = attr(Selector.Attr.URL)

        // created_utc
        val created = attr(Selector.Attr.TIMESTAMP).toLongOrNull()?.toSeconds() ?: 0L

        // is_gallery
        val isRedditGallery = attr(Selector.Attr.IS_GALLERY).toBoolean()

        val thumbnailClass = selectFirst("a.thumbnail")
        // is_video
        val isVideo = thumbnailClass?.selectFirst("div.duration-overlay")?.let { true } ?: false
        val thumbnail = thumbnailClass
            ?.selectFirst(Scraper.Selector.Tag.IMG)
            ?.attr(Scraper.Selector.Attr.SRC)
            ?.toValidLink()

        val expando = selectFirst("div.expando")
            ?.attr(Selector.Attr.CACHED_HTML)
            ?.run { Jsoup.parse(this) }

        val media = when {
            isVideo -> {
                Media(
                    null,
                    null,
                    RedditVideoPreview(
                        "$url/DASH_720.mp4",
                        0,
                        0,
                        0,
                        false
                    )
                )
            }

            expando != null -> expando.toMedia()

            else -> null
        }

        val galleryData = expando?.toGalleryData()
        val mediaMetadata = expando?.toMediaMetadata()

        val selfTextHtml = selectFirst("div.usertext-body")
            ?.selectFirst(Selector.MD)?.outerHtml()

        val tagline = getTagline()

        val postData = PostData(
            subreddit,
            flairRichText,
            authorFlairRichText = null,
            title,
            prefixedSubreddit,
            name,
            null,
            tagline.totalAwards,
            isOC,
            flair,
            null,
            galleryData,
            score,
            null,
            isSelf,
            null,
            domain,
            selfTextHtml,
            null,
            false,
            isOver18,
            null,
            tagline.awardings,
            isSpoiler,
            isLocked,
            tagline.distinguished,
            tagline.author,
            commentsNumber,
            permalink,
            isStickied,
            url,
            created,
            media,
            mediaMetadata,
            isRedditGallery,
            isVideo
        ).apply {
            this.thumbnail = thumbnail
            this.crosspost = crosspost
        }

        return PostChild(postData)
    }

    private fun Document.toMedia(): Media? {
        val source = selectFirst("source") ?: return null

        return when (source.attr("type")) {
            "video/mp4" -> {
                val src = source.attr(Scraper.Selector.Attr.SRC)

                Media(
                    null,
                    null,
                    RedditVideoPreview(
                        src,
                        0,
                        0,
                        0,
                        true
                    )
                )
            }

            else -> null
        }
    }

    private fun Document.toGalleryData(): GalleryData {
        val items = select("div.gallery-tile")
            .map {
                val id = it.attr(Selector.Attr.MEDIA_ID)
                GalleryDataItem(null, id)
            }

        return GalleryData(items)
    }

    private fun Document.toMediaMetadata(): MediaMetadata? {
        val items = select("div.gallery-preview")
            .map {
                val id = it.attr(Scraper.Selector.Attr.ID).substringAfterLast("-")
                val src = it.selectFirst("div.media-preview-content")
                    ?.selectFirst(Scraper.Selector.Tag.A)
                    ?.attr(Scraper.Selector.Attr.HREF)

                val image = GalleryImage(0, 0, src, null)

                GalleryItem(null, image, null, id)
            }

        return if (items.isNotEmpty()) MediaMetadata(items) else null
    }

    companion object {
        private const val KIND = "t3"
    }
}
