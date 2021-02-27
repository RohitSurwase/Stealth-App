package com.cosmos.unreddit.api.imgur.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data(
    @Json(name = "count")
    val count: Int,

    @Json(name = "images")
    val images: List<Image>
)
