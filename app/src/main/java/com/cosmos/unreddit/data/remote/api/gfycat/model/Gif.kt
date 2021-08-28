package com.cosmos.unreddit.data.remote.api.gfycat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Gif(
    @Json(name = "width")
    val width: Int,
    @Json(name = "size")
    val size: Int,
    @Json(name = "url")
    val url: String,
    @Json(name = "height")
    val height: Int
)
