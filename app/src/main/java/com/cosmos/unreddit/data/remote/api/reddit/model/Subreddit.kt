package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Subreddit(
    @Json(name = "banner_img")
    val bannerImg: String?,

    @Json(name = "community_icon")
    val communityIcon: String?,

    @Json(name = "icon_color")
    val iconColor: String,

    @Json(name = "header_img")
    val headerImg: String?,

    @Json(name = "title")
    val title: String,

    @Json(name = "over_18")
    val over18: Boolean,

    @Json(name = "primary_color")
    val primaryColor: String,

    @Json(name = "icon_img")
    val iconImg: String,

    @Json(name = "description")
    val description: String,

    @Json(name = "subscribers")
    val subscribers: Int,

    @Json(name = "display_name_prefixed")
    val displayNamePrefixed: String,

    @Json(name = "key_color")
    val keyColor: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "is_default_banner")
    val isDefaultBanner: Boolean,

    @Json(name = "url")
    val url: String,

    @Json(name = "public_description")
    val publicDescription: String
)
