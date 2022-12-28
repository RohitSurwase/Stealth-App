package com.cosmos.unreddit.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class ServiceExternal(
    @Json(name = "service")
    val service: String,

    @Json(name = "name")
    val name: String?,

    @Json(name = "pattern")
    val pattern: String,

    @Json(name = "redirect")
    val redirect: List<ServiceRedirect>
) : Parcelable
