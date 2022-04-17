package com.cosmos.unreddit.data.model.backup

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "name")
    val name: String,

    @Json(name = "subscriptions")
    val subscriptions: List<Subscription>,

    @Json(name = "saved_posts")
    val savedPosts: List<Post>? = null,

    @Json(name = "saved_comments")
    val savedComments: List<Comment>? = null,
)
