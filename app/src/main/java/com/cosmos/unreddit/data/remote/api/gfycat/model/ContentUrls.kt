package com.cosmos.unreddit.data.remote.api.gfycat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentUrls(
    @Json(name = "mp4")
    val mp4: Gif
)
