package com.cosmos.unreddit.api.pojo

import com.cosmos.unreddit.api.pojo.details.Child
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data(
    @Json(name = "things")
    val things: List<Child>
)
