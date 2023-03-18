package com.cosmos.unreddit.data.remote.api.redgifs.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Gif(
    @Json(name = "id")
    val id: String,

    @Json(name = "createDate")
    val createDate: Int,

    @Json(name = "hasAudio")
    val hasAudio: Boolean,

    @Json(name = "width")
    val width: Int,

    @Json(name = "height")
    val height: Int,

    @Json(name = "duration")
    val duration: Double,

    @Json(name = "urls")
    val urls: Urls
)
