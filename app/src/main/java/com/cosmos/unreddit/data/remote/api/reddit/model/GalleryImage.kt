package com.cosmos.unreddit.data.remote.api.reddit.model

import com.cosmos.unreddit.data.model.GalleryMedia
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryImage(
    @Json(name = "y")
    val height: Int,

    @Json(name = "x")
    val width: Int,

    @Json(name = "u")
    val url: String?,

    @Json(name = "mp4")
    val mp4: String?
) {
    val media: GalleryMedia?
        get() = when {
            url != null -> {
                GalleryMedia(GalleryMedia.Type.IMAGE, url)
            }
            mp4 != null -> {
                GalleryMedia(GalleryMedia.Type.VIDEO, mp4)
            }
            else -> null
        }
}
