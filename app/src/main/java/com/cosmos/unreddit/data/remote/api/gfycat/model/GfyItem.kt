package com.cosmos.unreddit.data.remote.api.gfycat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GfyItem(
    @Json(name = "content_urls")
    val contentUrls: ContentUrls
)
