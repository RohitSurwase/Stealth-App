package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Awarding (
    @Json(name = "icon_url")
    val url: String,

    @Json(name = "resized_icons")
    val resizedIcons: List<ImageSource>,

    @Json(name = "count")
    val count: Int
) {
    fun getIcon(): String {
        return resizedIcons.getOrNull(3)?.url ?: url // 64x64 icon
    }
}