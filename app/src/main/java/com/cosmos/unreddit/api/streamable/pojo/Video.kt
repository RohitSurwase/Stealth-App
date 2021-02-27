package com.cosmos.unreddit.api.streamable.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Video(
    @Json(name = "url")
    val url: String,

    @Json(name = "files")
    val files: Files
)
