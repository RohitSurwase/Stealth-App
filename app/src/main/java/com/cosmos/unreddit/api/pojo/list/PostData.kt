package com.cosmos.unreddit.api.pojo.list

import com.cosmos.unreddit.post.PostType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

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

    @Json(name = "is_video")
    val isVideo: Boolean
) {
    fun getPostType(): PostType {
        if (isSelf) {
            return PostType.TEXT
        }

        if (isVideo
            || media?.redditVideoPreview != null
            || mediaPreview?.videoPreview != null) {
            return PostType.VIDEO
        }

        hint?.let {
            if (it.contains("image")) {
                return PostType.IMAGE
            }
        }

        return PostType.LINK
    }

    fun getPreviewUrl(): String {
        return mediaPreview?.images?.get(0)?.imageSource?.url ?: url
    }

    fun getTimeInMillis(): Long {
        return TimeUnit.SECONDS.toMillis(created)
    }
}
