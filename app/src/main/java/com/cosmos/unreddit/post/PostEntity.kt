package com.cosmos.unreddit.post

import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.cosmos.unreddit.R
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.RedditText
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.formatNumber
import com.cosmos.unreddit.util.getPercentageValue
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

@Parcelize
@Entity(tableName = "saved_posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "subreddit")
    val subreddit: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "ratio")
    val ratio: Double,

    @ColumnInfo(name = "total_awards")
    val totalAwards: Int,

    @ColumnInfo(name = "oc")
    val isOC: Boolean,

    @Ignore
    val flair: Flair,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "type")
    val type: PostType,

    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "self")
    val isSelf: Boolean,

    @ColumnInfo(name = "selftext")
    val selfText: String?,

    @ColumnInfo(name = "selftext_html")
    val selfTextHtml: String?,

    @Ignore
    val suggestedSorting: Sorting,

    @Ignore
    val selfRedditText: RedditText,

    @ColumnInfo(name = "pinned")
    val isPinned: Boolean,

    @ColumnInfo(name = "nsfw")
    val isOver18: Boolean,

    @ColumnInfo(name = "preview_url")
    val preview: String?,

    @Ignore
    val awards: List<Award>,

    @ColumnInfo(name = "spoiler")
    val isSpoiler: Boolean,

    @ColumnInfo(name = "archived")
    val isArchived: Boolean,

    @ColumnInfo(name = "locked")
    val isLocked: Boolean,

    @ColumnInfo(name = "poster_type")
    val posterType: PosterType,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "comments")
    val commentsNumber: Int,

    @ColumnInfo(name = "permalink")
    val permalink: String,

    @ColumnInfo(name = "stickied")
    val isStickied: Boolean,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "timestamp")
    val created: Long,

    var seen: Boolean
) : Parcelable {

    constructor(id: String, subreddit: String, title: String, ratio: Double, totalAwards: Int,
                isOC: Boolean, score: Int, type: PostType, domain: String,
                isSelf: Boolean, selfText: String?, selfTextHtml: String?, isPinned: Boolean,
                isOver18: Boolean, preview: String?, isSpoiler: Boolean, isArchived: Boolean,
                isLocked: Boolean, posterType: PosterType, author: String, commentsNumber: Int,
                permalink: String, isStickied: Boolean, url: String, created: Long, seen: Boolean)
            : this(id, subreddit, title, ratio, totalAwards, isOC, Flair(), score, type, domain,
        isSelf, selfText, selfTextHtml, Sorting(RedditApi.Sort.BEST), RedditText(), isPinned,
        isOver18, preview, listOf(), isSpoiler, isArchived, isLocked, posterType, author,
        commentsNumber, permalink, isStickied, url, created, seen)

    fun getRatioColor(context: Context): Int {
        val low = ContextCompat.getColor(context, R.color.ratio_gradient_low)
        val high = ContextCompat.getColor(context, R.color.ratio_gradient_high)

        val r = ratio.getPercentageValue(Color.red(low), Color.red(high))
        val g = ratio.getPercentageValue(Color.green(low), Color.green(high))
        val b = ratio.getPercentageValue(Color.blue(low), Color.blue(high))

        return Color.rgb(r.roundToInt(), g.roundToInt(), b.roundToInt())
    }

    fun getVoteCount(): String {
        return score.formatNumber()
    }

    fun getCommentCount(): String {
        return commentsNumber.formatNumber()
    }

    fun getSeenColor(context: Context): Int {
        return if (seen)
            ContextCompat.getColor(context, R.color.text_color_post_seen)
        else
            ContextCompat.getColor(context, R.color.text_color) // TODO
    }

    fun getAuthorGradientColors(context: Context): IntArray {
        return when (posterType) {
            PosterType.REGULAR -> PostUtil.getAuthorGradientColor(
                context,
                R.color.regular_gradient_start,
                R.color.regular_gradient_end
            )
            PosterType.ADMIN -> PostUtil.getAuthorGradientColor(
                context,
                R.color.admin_gradient_start,
                R.color.admin_gradient_end
            )
            PosterType.MODERATOR -> PostUtil.getAuthorGradientColor(
                context,
                R.color.moderator_gradient_start,
                R.color.moderator_gradient_end
            )
        }
    }

    fun hasFlairs(): Boolean {
        return isOver18 || isSpoiler || isOC || !flair.isEmpty() || isStickied || isArchived ||
                isLocked
    }
}
