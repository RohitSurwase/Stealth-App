package com.cosmos.unreddit.data.remote.api.redgifs.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Token(
    @Json(name = "token")
    val token: String,

    @Json(name = "addr")
    val addr: String,

    @Json(name = "agent")
    val agent: String,

    @Json(name = "rtfm")
    val rtfm: String
)
