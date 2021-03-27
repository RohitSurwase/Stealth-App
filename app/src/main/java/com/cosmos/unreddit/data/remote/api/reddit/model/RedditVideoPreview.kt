package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RedditVideoPreview (
    @Json(name = "fallback_url")
    val fallbackUrl: String,

    @Json(name = "height")
    val height: Int,

    @Json(name = "width")
    val width: Int,

    @Json(name = "duration")
    val duration: Int,

    @Json(name = "is_gif")
    val isGif: Boolean
)