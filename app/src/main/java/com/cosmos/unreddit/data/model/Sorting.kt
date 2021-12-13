package com.cosmos.unreddit.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sorting(
    val generalSorting: Sort,

    val timeSorting: TimeSorting? = null
) : Parcelable
