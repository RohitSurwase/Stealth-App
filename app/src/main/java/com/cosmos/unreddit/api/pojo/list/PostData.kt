package com.cosmos.unreddit.api.pojo.list

import com.cosmos.unreddit.api.pojo.MediaMetadata
import com.cosmos.unreddit.model.GalleryMedia
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.post.PostType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostData(
    @Json(name = "subreddit")
    val subreddit: String,

    @Json(name = "selftext")
    val selfText: String?,

    @Json(name = "link_flair_richtext")
    val linkFlairRichText: List<RichText>,

    @Json(name = "author_flair_richtext")
    val authorFlairRichText: List<RichText>?,

    @Json(name = "title")
    val title: String,

    @Json(name = "subreddit_name_prefixed")
    val prefixedSubreddit: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "upvote_ratio")
    val ratio: Double,

    @Json(name = "total_awards_received")
    val totalAwards: Int,

    @Json(name = "is_original_content")
    val isOC: Boolean,

    @Json(name = "link_flair_text")
    val flair: String?,

    @Json(name = "author_flair_text")
    val authorFlair: String?,

    @Json(name = "score")
    val score: Int,

    @Json(name = "post_hint")
    val hint: String?,

    @Json(name = "is_self")
    val isSelf: Boolean,

    @Json(name = "domain")
    val domain: String,

    @Json(name = "selftext_html")
    val selfTextHtml: String?,

    @Json(name = "suggested_sort")
    val suggestedSort: String?,

    @Json(name = "archived")
    val isArchived: Boolean,

    @Json(name = "pinned")
    val isPinned: Boolean,

    @Json(name = "over_18")
    val isOver18: Boolean,

    @Json(name = "preview")
    val mediaPreview: MediaPreview?,

    @Json(name = "all_awardings")
    val awardings: List<Awarding>,

    @Json(name = "spoiler")
    val isSpoiler: Boolean,

    @Json(name = "locked")
    val isLocked: Boolean,

    @Json(name = "distinguished")
    val distinguished: String?,

    @Json(name = "author")
    val author: String,

    @Json(name = "num_comments")
    val commentsNumber: Int,

    @Json(name = "permalink")
    val permalink: String,

    @Json(name = "stickied")
    val isStickied: Boolean,

    @Json(name = "url")
    val url: String,

    @Json(name = "created_utc")
    val created: Long,

    @Json(name = "media")
    val media: Media?,

    @Json(name = "media_metadata")
    val mediaMetadata: MediaMetadata?,

    @Json(name = "is_gallery")
    val isRedditGallery: Boolean?,

    @Json(name = "is_video")
    val isVideo: Boolean
) {
    val mediaType: MediaType
        get() = when {
            isSelf -> MediaType.NO_MEDIA
            isRedditGallery == true -> MediaType.REDDIT_GALLERY
            isVideo -> {
                if (media?.redditVideoPreview?.isGif == true) {
                    MediaType.REDDIT_GIF
                } else {
                    MediaType.REDDIT_VIDEO
                }
            }
            domain == "imgur.com" || domain == "m.imgur.com" -> {
                when {
                    url.contains("imgur.com/a/") -> MediaType.IMGUR_ALBUM
                    url.contains("imgur.com/gallery/") -> MediaType.IMGUR_GALLERY
                    url.endsWith(".gifv") || url.endsWith(".gif") -> MediaType.IMGUR_GIF
                    url.endsWith(".mp4") -> MediaType.IMGUR_VIDEO
                    else -> MediaType.IMGUR_LINK
                }
            }
            domain == "i.imgur.com" -> {
                when {
                    url.endsWith(".gifv") || url.endsWith(".gif") -> MediaType.IMGUR_GIF
                    url.endsWith(".mp4") -> MediaType.IMGUR_VIDEO
                    else -> MediaType.IMGUR_IMAGE
                }
            }
            domain == "gfycat.com" -> MediaType.GFYCAT
            domain == "redgifs.com" -> MediaType.REDGIFS
            domain == "streamable.com" -> MediaType.STREAMABLE
            domain == "v.redd.it" -> {
                if (media?.redditVideoPreview?.isGif == true) {
                    MediaType.REDDIT_GIF
                } else {
                    MediaType.REDDIT_VIDEO
                }
            }
            media?.redditVideoPreview != null ||
                    mediaPreview?.videoPreview != null ||
                    url.endsWith(".gif") ||
                    url.endsWith(".gifv") ||
                    url.endsWith(".mp4") ||
                    url.endsWith(".webm") -> {
                MediaType.VIDEO
            }
            domain == "i.redd.it" -> MediaType.IMAGE
            hint?.contains("image") == true -> MediaType.IMAGE
            else -> MediaType.LINK
        }

    val mediaUrl: String
        get() = when (mediaType) {
            MediaType.REDDIT_VIDEO, MediaType.REDDIT_GIF -> {
                media?.redditVideoPreview?.fallbackUrl
                    ?: mediaPreview?.videoPreview?.fallbackUrl
            }
            MediaType.VIDEO -> {
                media?.redditVideoPreview?.fallbackUrl
                    ?: mediaPreview?.videoPreview?.fallbackUrl
                    ?: mediaPreview?.images?.getOrNull(0)?.variants?.mp4?.imageSource?.url
            }
            MediaType.IMGUR_LINK -> mediaPreview?.images?.getOrNull(0)?.imageSource?.url
            MediaType.GFYCAT -> {
                media?.embed?.thumbnailUrl ?: mediaPreview?.videoPreview?.fallbackUrl
            }
            else -> url
        } ?: url

    val postType: PostType
        get() = when (mediaType) {
            MediaType.NO_MEDIA -> PostType.TEXT

            MediaType.REDDIT_VIDEO,
            MediaType.REDDIT_GIF,
            MediaType.IMGUR_VIDEO,
            MediaType.IMGUR_GIF,
            MediaType.GFYCAT,
            MediaType.REDGIFS,
            MediaType.STREAMABLE,
            MediaType.VIDEO -> PostType.VIDEO

            MediaType.REDDIT_GALLERY,
            MediaType.IMGUR_GALLERY,
            MediaType.IMGUR_ALBUM,
            MediaType.IMGUR_IMAGE,
            MediaType.IMGUR_LINK,
            MediaType.IMAGE -> PostType.IMAGE

            else -> PostType.LINK
        }

    val previewUrl: String
        get() = mediaPreview?.images?.getOrNull(0)?.imageSource?.url
            ?: mediaMetadata?.items?.getOrNull(0)?.image?.url
            ?: mediaMetadata?.items?.getOrNull(0)?.previews?.last()?.url
            ?: url

    val gallery: List<GalleryMedia>
        get() = mediaMetadata?.items?.mapNotNull { item ->
            item.image?.let {
                when {
                    it.url != null -> {
                        GalleryMedia(GalleryMedia.Type.IMAGE, it.url)
                    }
                    it.mp4 != null -> {
                        GalleryMedia(GalleryMedia.Type.VIDEO, it.mp4)
                    }
                    else -> null
                }
            }
        } ?: emptyList()
}
