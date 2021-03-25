package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryItem(
    @Json(name = "m")
    val mimeType: String?,

    @Json(name = "s")
    val image: GalleryImage?,

    @Json(name = "p")
    val previews: List<GalleryImage>?,

    @Json(name = "id")
    val id: String
)
