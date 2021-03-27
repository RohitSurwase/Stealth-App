package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MoreData (
    @Json(name = "count")
    val count: Int,

    @Json(name = "name")
    val name: String,

    @Json(name = "id")
    val id: String,

    @Json(name = "depth")
    val depth: Int?,

    @Json(name = "parent_id")
    val parentId: String,

    @Json(name = "children")
    val children: MutableList<String>
)
