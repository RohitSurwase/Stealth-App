package com.cosmos.unreddit.data.model

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.util.DateUtil
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

sealed class Comment {

    abstract val name: String
    abstract val depth: Int

    @Parcelize
    @Entity(
        tableName = "comment",
        primaryKeys = ["name", "profile_id"],
        foreignKeys = [
            ForeignKey(
                entity = Profile::class,
                parentColumns = ["id"],
                childColumns = ["profile_id"],
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
    data class CommentEntity @JvmOverloads constructor(
        @ColumnInfo(name = "total_awards")
        val totalAwards: Int,

        @ColumnInfo(name = "link_id")
        val linkId: String,

        @Ignore
        val replies: @RawValue MutableList<Comment> = mutableListOf(),

        val author: String,

        val score: String,

        @Ignore
        val awards: List<Award> = listOf(),

        @ColumnInfo(name = "body_html")
        val bodyHtml: String,

        @Ignore
        var body: RedditText = RedditText(),

        val edited: Long,

        @ColumnInfo(name = "submitter")
        val isSubmitter: Boolean,

        val stickied: Boolean,

        @ColumnInfo(name = "score_hidden")
        val scoreHidden: Boolean,

        val permalink: String,

        val id: String,

        val created: Long,

        val controversiality: Int,

        @Ignore
        val flair: Flair = Flair(),

        @ColumnInfo(name = "poster_type")
        val posterType: PosterType,

        @ColumnInfo(name = "link_title")
        val linkTitle: String?,

        @ColumnInfo(name = "link_permalink")
        val linkPermalink: String?,

        @ColumnInfo(name = "link_author")
        val linkAuthor: String?,

        val subreddit: String,

        @Ignore
        val commentIndicator: Int? = null,

        @ColumnInfo(name = "name")
        override val name: String,

        @Ignore
        override val depth: Int = 0,

        @Ignore
        var saved: Boolean = true,

        @ColumnInfo(name = "time")
        var time: Long = -1,

        @ColumnInfo(name = "profile_id", index = true)
        var profileId: Int = -1,
    ) : Comment(), Parcelable {

        @Ignore
        @IgnoredOnParcel
        var isExpanded: Boolean = false

        @Ignore
        @IgnoredOnParcel
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
    ) : Comment() {
        var isLoading: Boolean = false

        var isError: Boolean = false
    }
}
