package com.cosmos.unreddit.data.model

import android.os.Parcelable
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sorting(
    val generalSorting: RedditApi.Sort,

    val timeSorting: RedditApi.TimeSorting? = null
) : Parcelable
