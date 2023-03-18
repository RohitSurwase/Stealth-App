package com.cosmos.unreddit.data.remote.api.gfycat

import com.cosmos.unreddit.data.remote.api.gfycat.model.Item
import retrofit2.http.GET
import retrofit2.http.Path

interface GfycatApi {

    @GET("gfycats/{id}")
    suspend fun getGif(@Path("id") id: String): Item

    companion object {
        const val BASE_URL_GFYCAT = "https://api.gfycat.com/v1/"
    }
}
