package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TedditUser(
    @Json(name = "about")
    val about: AboutUserChild,

    @Json(name = "overview")
    val overview: Listing
)
