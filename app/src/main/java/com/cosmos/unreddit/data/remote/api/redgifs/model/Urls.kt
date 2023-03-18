package com.cosmos.unreddit.data.remote.api.redgifs.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Urls(
    @Json(name = "poster")
    val poster: String,

    @Json(name = "thumbnail")
    val thumbnail: String,

    @Json(name = "vthumbnail")
    val vthumbnail: String,

    @Json(name = "hd")
    val hd: String,

    @Json(name = "sd")
    val sd: String
)
