package com.cosmos.unreddit.api.pojo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryItem(
    @Json(name = "m")
    val mimeType: String,

    @Json(name = "s")
    var image: GalleryImage? = null,

    @Json(name = "id")
    val id: String
)
