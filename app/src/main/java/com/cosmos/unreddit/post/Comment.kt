package com.cosmos.unreddit.post

import android.content.Context
import androidx.annotation.ColorRes
import com.cosmos.unreddit.R
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.RedditText
import kotlin.math.abs
import kotlin.math.truncate

interface Comment {
    val name: String
    val depth: Int
}

data class CommentEntity(
    val totalAwards: Int,

    val linkId: String,

    val replies: MutableList<Comment>,

    val author: String,

    val score: Int,

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

    override val name: String,

    override val depth: Int
) : Comment {
    var isExpanded: Boolean = false

    var visibleReplyCount: Int = replies.size

    val hasReplies: Boolean
        get() = replies.isNotEmpty()

    fun getVoteCount(): String {
        return when {
            scoreHidden -> "12345" // TODO: Blur score when hidden
            score < 1000 -> score.toString()
            else -> {
                val roundedScore = String.format("%.1f", score.div(1000f))
                "${roundedScore}k"
            }
        }
    }

    @ColorRes
    fun getIndicatorColor(context: Context): Int? {
        if (depth <= 0) return null

        val colorArray = context.resources.getIntArray(R.array.comment_indicator)
        val arrayBound = colorArray.size - 1
        val commentDepth = depth - 1

        return if (commentDepth in colorArray.indices) {
            colorArray[commentDepth]
        } else {
            val index = truncate(abs((arrayBound - commentDepth) / arrayBound).toDouble()).toInt()
            colorArray[index]
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
