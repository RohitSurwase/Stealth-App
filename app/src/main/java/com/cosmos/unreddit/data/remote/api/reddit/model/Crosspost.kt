package com.cosmos.unreddit.data.remote.api.reddit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Crosspost(
    val author: String?,
    val subreddit: String?,
    val title: String?,
    val date: Long?
) : Parcelable
