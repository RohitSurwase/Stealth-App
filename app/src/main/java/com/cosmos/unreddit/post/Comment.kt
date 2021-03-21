package com.cosmos.unreddit.post

import android.content.Context
import com.cosmos.unreddit.R
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.RedditText
import com.cosmos.unreddit.util.DateUtil

interface Comment {
    val name: String
    val depth: Int
}

data class CommentEntity(
    val totalAwards: Int,

    val linkId: String,

    val replies: MutableList<Comment>,

    val author: String,

    val score: String,

    val awards: List<Award>,

    val body: RedditText,

    val edited: Long,

    val isSubmitter: Boolean,

    val stickied: Boolean,

    val scoreHidden: Boolean,

    val permalink: String,

    val id: String,

    val created: Long,

    val controversiality: Int,

    val flair: Flair,

    val posterType: PosterType,

    val linkTitle: String?,

    val linkPermalink: String?,

    val linkAuthor: String?,

    val subreddit: String,

    val commentIndicator: Int?,

    override val name: String,

    override val depth: Int
) : Comment {
    var isExpanded: Boolean = false

    var visibleReplyCount: Int = replies.size

    val hasReplies: Boolean
        get() = replies.isNotEmpty()

    fun getTimeDifference(context: Context): String {
        val timeDifference = DateUtil.getTimeDifference(context, created)
        return if (edited > -1) {
            val editedTimeDifference = DateUtil.getTimeDifference(context, edited, false)
            context.getString(R.string.comment_date_edited, timeDifference, editedTimeDifference)
        } else {
            timeDifference
        }
    }
}

data class MoreEntity(
    var count: Int,

    val more: MutableList<String>,

    val id: String,

    val parent: String,

    override val name: String,

    override val depth: Int
) : Comment {
    var isLoading: Boolean = false

    var isError: Boolean = false
}

fun Comment.getType(): CommentType = when (this) {
    is CommentEntity -> CommentType.COMMENT
    is MoreEntity -> CommentType.MORE
    else -> throw IllegalArgumentException("Unknown type ${javaClass.simpleName}")
}
