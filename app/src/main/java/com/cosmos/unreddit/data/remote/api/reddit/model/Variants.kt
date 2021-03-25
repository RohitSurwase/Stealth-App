package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Variants (
    @Json(name = "gif")
    val gif: Image?,

    @Json(name = "mp4")
    val mp4: Image?
)