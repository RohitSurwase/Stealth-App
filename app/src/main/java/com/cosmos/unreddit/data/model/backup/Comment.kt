package com.cosmos.unreddit.data.model.backup

import com.cosmos.unreddit.data.model.PosterType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Comment(
    @Json(name = "total_awards")
    val totalAwards: Int,

    @Json(name = "link_id")
    val linkId: String,

    @Json(name = "author")
    val author: String,

    @Json(name = "score")
    val score: String,

    @Json(name = "body_html")
    val bodyHtml: String,

    @Json(name = "edited")
    val edited: Long,

    @Json(name = "submitter")
    val isSubmitter: Boolean,

    @Json(name = "stickied")
    val stickied: Boolean,

    @Json(name = "score_hidden")
    val scoreHidden: Boolean,

    @Json(name = "permalink")
    val permalink: String,

    @Json(name = "id")
    val id: String,

    @Json(name = "created")
    val created: Long,

    @Json(name = "controversiality")
    val controversiality: Int,

    @Json(name = "poster_type")
    val posterType: PosterType,

    @Json(name = "link_title")
    val linkTitle: String?,

    @Json(name = "link_permalink")
    val linkPermalink: String?,

    @Json(name = "link_author")
    val linkAuthor: String?,

    @Json(name = "subreddit")
    val subreddit: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "time")
    var time: Long = -1,
)
