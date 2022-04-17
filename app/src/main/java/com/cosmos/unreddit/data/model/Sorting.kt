package com.cosmos.unreddit.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Sorting(
    @Json(name = "general_sorting")
    val generalSorting: Sort,

    @Json(name = "time_sorting")
    val timeSorting: TimeSorting? = null
) : Parcelable
