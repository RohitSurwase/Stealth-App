package com.cosmos.unreddit.data.remote.api.imgur.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Album(
    @Json(name = "data")
    val data: Data,

    @Json(name = "success")
    val success: Boolean,

    @Json(name = "status")
    val status: Int
)
