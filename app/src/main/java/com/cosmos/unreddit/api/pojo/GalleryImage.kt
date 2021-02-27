package com.cosmos.unreddit.api.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryImage(
    @Json(name = "y")
    var height: Int,

    @Json(name = "x")
    var width: Int,

    @Json(name = "u")
    var url: String
)
