package com.cosmos.unreddit.api.imgur.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Image(
    @Json(name = "hash")
    val hash: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "size")
    val size: Int,

    @Json(name = "ext")
    val ext: String,

    @Json(name = "prefer_video")
    val preferVideo: Boolean
)
