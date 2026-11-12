package com.cosmos.unreddit.data.remote.api.redlib

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of the public Redlib instance list:
 * https://github.com/redlib-org/redlib-instances
 */
@JsonClass(generateAdapter = true)
data class RedlibInstances(
    @Json(name = "updated")
    val updated: String? = null,

    @Json(name = "instances")
    val instances: List<RedlibInstance> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RedlibInstance(
    /**
     * Clearnet URL. Onion/I2P entries use [onion]/[i2p] instead.
     */
    @Json(name = "url")
    val url: String? = null,

    @Json(name = "onion")
    val onion: String? = null,

    @Json(name = "i2p")
    val i2p: String? = null
)
