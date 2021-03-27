package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ListingData (
    @Json(name = "modhash")
    val modhash: String?,

    @Json(name = "dist")
    val dist: Int?,

    @Json(name = "children")
    val children: List<Child>,

    @Json(name = "after")
    val after: String?,

    @Json(name = "before")
    val before: String?
)