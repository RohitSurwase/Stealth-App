package com.cosmos.unreddit.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class ServiceRedirect(
    @Json(name = "name")
    val name: String,

    @Json(name = "instances")
    val instances: List<String>
) : Parcelable
