package com.cosmos.unreddit.api.pojo

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class MediaMetadata(
    val items: List<GalleryItem>
)
