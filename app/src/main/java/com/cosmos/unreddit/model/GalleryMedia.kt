package com.cosmos.unreddit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GalleryMedia(
    val type: Type,

    val url: String,

    val description: String? = null
) : Parcelable {

    enum class Type {
        IMAGE, VIDEO
    }
}
