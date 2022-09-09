package com.cosmos.unreddit.data.remote.api.reddit.model

import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.remote.api.reddit.adapter.NullToEmptyString
import com.cosmos.unreddit.util.extension.mimeType
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

    @Json(name = "gallery_data")
    val galleryData: GalleryData?,

    @Json(name = "score")
    val score: Int,

    @Json(name = "post_hint")
    val hint: String?,

    @Json(name = "is_self")
    val isSelf: Boolean,

    @Json(name = "crosspost_parent_list")
    val crossposts: List<PostData>?,

    @Json(name = "domain")
    @NullToEmptyString
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
    @NullToEmptyString
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
    val mediaType: MediaType =
        when {
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

    val mediaUrl: String =
        when (mediaType) {
            MediaType.REDDIT_VIDEO, MediaType.REDDIT_GIF -> {
                crossposts?.firstOrNull()?.mediaUrl
                    ?: media?.redditVideoPreview?.fallbackUrl
                    ?: mediaPreview?.videoPreview?.fallbackUrl
            }
            MediaType.VIDEO -> {
                crossposts?.firstOrNull()?.mediaUrl
                    ?: media?.redditVideoPreview?.fallbackUrl
                    ?: mediaPreview?.videoPreview?.fallbackUrl
                    ?: mediaPreview?.images?.getOrNull(0)?.variants?.mp4?.imageSource?.url
            }
            MediaType.IMGUR_LINK -> {
                crossposts?.firstOrNull()?.mediaUrl
                    ?: mediaPreview?.images?.getOrNull(0)?.imageSource?.url
            }
            else -> url
        } ?: url

    val postType: PostType =
        when (mediaType) {
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

    val previewUrl: String? =
        mediaPreview?.images?.getOrNull(0)?.imageSource?.url
            ?: mediaMetadata?.items?.getOrNull(0)?.image?.url
            ?: mediaMetadata?.items?.getOrNull(0)?.previews?.lastOrNull()?.url
            // Keep URL only if it's an image
            ?: url.takeIf { postType != PostType.LINK || it.mimeType.startsWith("image") }

    // Reddit's API provides two lists of gallery items:
    // - gallery_data which is ordered but only contains IDs and captions
    // - media_metadata which contains all the URLs but is unordered
    // Therefore, we need to map the IDs from gallery_data with the URLs from media_metadata
    // to have an ordered list of items
    val gallery: List<GalleryMedia> =
        galleryData?.items?.mapNotNull { galleryDataItem ->
            mediaMetadata?.items?.find { galleryItem ->
                galleryItem.id == galleryDataItem.mediaId
            }?.let { item ->
                item.image?.media
            }
        } ?: emptyList()
}
