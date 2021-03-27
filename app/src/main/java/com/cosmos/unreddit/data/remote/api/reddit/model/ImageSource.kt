package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageSource (
    @Json(name = "url")
    val url: String,

    @Json(name = "width")
    val width: Int,

    @Json(name = "height")
    val height: Int
)