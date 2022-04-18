package com.cosmos.unreddit.data.remote.api.reddit.model

import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.remote.api.reddit.adapter.Edited
import com.cosmos.unreddit.data.remote.api.reddit.adapter.Replies
import com.cosmos.unreddit.util.extension.fitToRange
import com.cosmos.unreddit.util.extension.formatNumber
import com.cosmos.unreddit.util.extension.toMillis
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CommentData(
    @Json(name = "total_awards_received")
    val totalAwards: Int,

    @Json(name = "author_flair_richtext")
    val authorFlairRichText: List<RichText>?,

    @Json(name = "link_id")
    val linkId: String,

    @Json(name = "replies")
    @Replies
    val replies: Listing?,

    @Json(name = "author")
    val author: String,

    @Json(name = "score")
    val score: Int,

    @Json(name = "all_awardings")
    val awardings: List<Awarding>,

    @Json(name = "body_html")
    val bodyHtml: String,

    @Json(name = "edited")
    @Edited
    val edited: Long,

    @Json(name = "is_submitter")
    val isSubmitter: Boolean,

    @Json(name = "stickied")
    val stickied: Boolean,

    @Json(name = "score_hidden")
    val scoreHidden: Boolean,

    @Json(name = "permalink")
    val permalink: String,

    @Json(name = "id")
    val id: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "created_utc")
    val created: Long,

    @Json(name = "controversiality")
    val controversiality: Int,

    @Json(name = "author_flair_text")
    val flair: String?,

    @Json(name = "depth")
    val depth: Int?,

    @Json(name = "distinguished")
    val distinguished: String?,

    @Json(name = "subreddit_name_prefixed")
    val subreddit: String,

    @Json(name = "link_title")
    val linkTitle: String?,

    @Json(name = "link_permalink")
    val linkPermalink: String?,

    @Json(name = "link_author")
    val linkAuthor: String?
) {
    val scoreString: String
        get() = if (scoreHidden) "12345" else score.formatNumber()

    val editedMillis: Long
        get() = if (edited > -1) edited.toMillis() else edited

    val commentIndicator: Int?
        get() {
            if (depth == null || depth <= 0) return null

            val commentDepth = depth - 1

            return if (commentDepth in colorArray.indices) {
                colorArray[commentDepth]
            } else {
                colorArray[commentDepth.fitToRange(colorArray.indices)]
            }
        }

    companion object {
        private val colorArray = arrayOf(
            R.color.comment_indicator_1,
            R.color.comment_indicator_2,
            R.color.comment_indicator_3,
            R.color.comment_indicator_4,
            R.color.comment_indicator_5,
            R.color.comment_indicator_6,
            R.color.comment_indicator_7,
            R.color.comment_indicator_8,
        )
    }
}
