package com.cosmos.unreddit.data.remote.api.redgifs.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Item(
    @Json(name = "gif")
    val gif: Gif
)
