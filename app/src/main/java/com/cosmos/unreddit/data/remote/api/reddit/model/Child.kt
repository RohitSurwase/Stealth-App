package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed class Child (
    @Json(name = "kind")
    val kind: ChildType
)

@JsonClass(generateAdapter = true)
data class CommentChild (
    @Json(name = "data")
    val data: CommentData
) : Child(ChildType.t1)

@JsonClass(generateAdapter = true)
data class AboutUserChild (
    @Json(name = "data")
    val data: AboutUserData
) : Child(ChildType.t2)

@JsonClass(generateAdapter = true)
data class PostChild (
    @Json(name = "data")
    val data: PostData
) : Child(ChildType.t3)

@JsonClass(generateAdapter = true)
data class AboutChild (
    @Json(name = "data")
    val data: AboutData
) : Child(ChildType.t5)

@JsonClass(generateAdapter = true)
data class MoreChild (
    @Json(name = "data")
    val data: MoreData
) : Child(ChildType.more)

// TODO: Rename
enum class ChildType {
    @Json(name = "t1")
    t1,

    @Json(name = "t2")
    t2,

    @Json(name = "t3")
    t3,

    @Json(name = "t5")
    t5,

    @Json(name = "more")
    more
}