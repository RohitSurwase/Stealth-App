package com.cosmos.unreddit.api.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonMore(
    @Json(name = "data")
    val data: Data
)
