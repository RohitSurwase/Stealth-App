package com.cosmos.unreddit.data.remote.api.redgifs

import com.cosmos.unreddit.data.remote.api.redgifs.model.Item
import com.cosmos.unreddit.data.remote.api.redgifs.model.Token
import com.cosmos.unreddit.util.LinkUtil
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

interface RedgifsApi {

    @Headers("User-Agent: ${LinkUtil.USER_AGENT}")
    @GET("/v2/auth/temporary")
    suspend fun getTemporaryToken(): Token

    @Headers("User-Agent: ${LinkUtil.USER_AGENT}")
    @GET("/v2/gifs/{id}")
    suspend fun getGif(@Header("authorization") authorization: String, @Path("id") id: String): Item

    companion object {
        const val BASE_URL = "https://api.redgifs.com/"
    }
}
