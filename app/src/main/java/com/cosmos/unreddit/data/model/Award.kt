package com.cosmos.unreddit.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Award(
    val count: Int,

    val icon: String
) : Parcelable
