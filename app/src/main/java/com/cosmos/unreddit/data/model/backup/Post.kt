package com.cosmos.unreddit.data.model.backup

import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.data.model.Sorting
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Post(
    @Json(name = "id")
    val id: String,

    @Json(name = "subreddit")
    val subreddit: String,

    @Json(name = "title")
    val title: String,

    @Json(name = "ratio")
    val ratio: Int,

    @Json(name = "total_awards")
    val totalAwards: Int,

    @Json(name = "oc")
    val isOC: Boolean,

    @Json(name = "score")
    val score: String,

    @Json(name = "type")
    val type: PostType,

    @Json(name = "domain")
    val domain: String,

    @Json(name = "self")
    val isSelf: Boolean,

    @Json(name = "self_text_html")
    val selfTextHtml: String?,

    @Json(name = "suggested_sorting")
    val suggestedSorting: Sorting,

    @Json(name = "nsfw")
    val isOver18: Boolean,

    @Json(name = "preview")
    val preview: String?,

    @Json(name = "spoiler")
    val isSpoiler: Boolean,

    @Json(name = "archived")
    val isArchived: Boolean,

    @Json(name = "locked")
    val isLocked: Boolean,

    @Json(name = "poster_type")
    val posterType: PosterType,

    @Json(name = "author")
    val author: String,

    @Json(name = "comments_number")
    val commentsNumber: String,

    @Json(name = "permalink")
    val permalink: String,

    @Json(name = "stickied")
    val isStickied: Boolean,

    @Json(name = "url")
    val url: String,

    @Json(name = "created")
    val created: Long,

    @Json(name = "media_type")
    val mediaType: MediaType,

    @Json(name = "media_url")
    val mediaUrl: String,

    @Json(name = "time")
    var time: Long = -1,
)
