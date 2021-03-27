package com.cosmos.unreddit.data.remote.api.reddit.model

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
)
