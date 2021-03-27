package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Embed(
    @Json(name = "url")
    val url: String?,

    @Json(name = "thumbnail_url")
    val thumbnailUrl: String?,

    @Json(name = "provider_name")
    val providerName: String
)
