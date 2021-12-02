package com.cosmos.unreddit.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class TargetRedditInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url()

        val newUrl = url.newBuilder()
            .addQueryParameter("target", "reddit")
            .build()

        val newRequest = request.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
