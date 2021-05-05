package com.cosmos.unreddit.data.model.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Award
import com.cosmos.unreddit.data.model.Flair
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.data.model.RedditText
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "post",
    primaryKeys = ["id", "profile_id"],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PostEntity @JvmOverloads constructor(
    @ColumnInfo(name = "id")
    val id: String,

    val subreddit: String,

    val title: String,

    val ratio: Int,

    @ColumnInfo(name = "total_awards")
    val totalAwards: Int,

    @ColumnInfo(name = "oc")
    val isOC: Boolean,

    @Ignore
    val flair: Flair = Flair(), // TODO: Handle flairs

    @Ignore
    val authorFlair: Flair = Flair(), // TODO: Handle flairs

    @Ignore
    var hasFlairs: Boolean = false,

    val score: String,

    val type: PostType,

    val domain: String,

    @ColumnInfo(name = "self")
    val isSelf: Boolean,

    @Ignore
    val crosspost: PostEntity? = null,

    @ColumnInfo(name = "self_text_html")
    val selfTextHtml: String?,

    @ColumnInfo(name = "suggested_sorting")
    val suggestedSorting: Sorting,

    @Ignore
    var selfRedditText: RedditText = RedditText(),

    @ColumnInfo(name = "nsfw")
    val isOver18: Boolean,

    val preview: String?,

    @Ignore
    val previewText: CharSequence? = null,

    @Ignore
    val awards: List<Award> = listOf(),

    @ColumnInfo(name = "spoiler")
    val isSpoiler: Boolean,

    @ColumnInfo(name = "archived")
    val isArchived: Boolean,

    @ColumnInfo(name = "locked")
    val isLocked: Boolean,

    @ColumnInfo(name = "poster_type")
    val posterType: PosterType,

    val author: String,

    @ColumnInfo(name = "comments_number")
    val commentsNumber: String,

    val permalink: String,

    @ColumnInfo(name = "stickied")
    val isStickied: Boolean,

    val url: String,

    val created: Long,

    @ColumnInfo(name = "media_type")
    val mediaType: MediaType,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String,

    @Ignore
    val gallery: List<GalleryMedia> = listOf(),

    @Ignore
    var seen: Boolean = true,

    @Ignore
    var saved: Boolean = true,

    @ColumnInfo(name = "time")
    var time: Long = -1,

    @ColumnInfo(name = "profile_id")
    var profileId: Int = -1,
) : Parcelable {

    val textColor: Int
        get() = if (seen) R.color.text_color_post_seen else R.color.text_color

    fun shouldShowPreview(contentPreferences: ContentPreferences): Boolean {
        return (contentPreferences.showNsfwPreview || !isOver18) &&
                (contentPreferences.showSpoilerPreview || !isSpoiler)
    }
}
