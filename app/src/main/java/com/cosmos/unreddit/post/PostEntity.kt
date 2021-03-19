package com.cosmos.unreddit.post

import android.os.Parcelable
import com.cosmos.unreddit.R
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.GalleryMedia
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.RedditText
import com.cosmos.unreddit.preferences.ContentPreferences
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostEntity(
    val id: String,

    val subreddit: String,

    val title: String,

    val ratio: Int,

    val totalAwards: Int,

    val isOC: Boolean,

    val flair: Flair,

    val authorFlair: Flair,

    val hasFlairs: Boolean,

    val score: String,

    val type: PostType,

    val domain: String,

    val isSelf: Boolean,

    val selfTextHtml: String?,

    val suggestedSorting: Sorting,

    val selfRedditText: RedditText,

    val isOver18: Boolean,

    val preview: String?,

    val previewText: CharSequence?,

    val awards: List<Award>,

    val isSpoiler: Boolean,

    val isArchived: Boolean,

    val isLocked: Boolean,

    val posterType: PosterType,

    val author: String,

    val commentsNumber: String,

    val permalink: String,

    val isStickied: Boolean,

    val url: String,

    val created: Long,

    val mediaType: MediaType,

    val mediaUrl: String,

    val gallery: List<GalleryMedia>,

    var seen: Boolean
) : Parcelable {

    val textColor: Int
        get() = if (seen) R.color.text_color_post_seen else R.color.text_color

    fun shouldShowPreview(contentPreferences: ContentPreferences): Boolean {
        return (contentPreferences.showNsfwPreview || !isOver18) &&
                (contentPreferences.showSpoilerPreview || !isSpoiler)
    }
}
