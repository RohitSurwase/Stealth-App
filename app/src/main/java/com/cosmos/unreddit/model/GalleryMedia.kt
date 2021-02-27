package com.cosmos.unreddit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GalleryMedia(
    val type: Type,

    val url: String,

    val sound: String? = null,

    val description: String? = null
) : Parcelable {

    enum class Type(val value: Int) {
        IMAGE(0), VIDEO(1)
    }

    companion object {
        fun singleton(
            type: Type,
            url: String,
            sound: String? = null,
            description: String? = null
        ): List<GalleryMedia> {
            return listOf(GalleryMedia(type, url, sound, description))
        }
    }
}
