package com.cosmos.unreddit.data.remote.api.gfycat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Item(
    @Json(name = "gfyItem")
    val gfyItem: GfyItem
)
