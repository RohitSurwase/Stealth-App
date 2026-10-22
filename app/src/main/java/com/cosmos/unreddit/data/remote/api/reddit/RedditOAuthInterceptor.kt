package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RedditOAuthInterceptor(
    private val oauthManager: RedditOAuthManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = oauthManager.getCredentials()
            ?: throw IOException("Unable to obtain Reddit OAuth token")

        val response = chain.proceed(authorizedRequest(chain.request(), credentials))
        if (response.code != 401) {
            return response
        }

        response.close()
        oauthManager.invalidate()

        val refreshedCredentials = oauthManager.getCredentials()
            ?: throw IOException("Unable to refresh Reddit OAuth token")
        return chain.proceed(authorizedRequest(chain.request(), refreshedCredentials))
    }

    private fun authorizedRequest(
        request: Request,
        credentials: RedditOAuthCredentials
    ): Request {
        return request.newBuilder()
            .header("Authorization", credentials.authorization)
            .header("User-Agent", credentials.userAgent)
            .header("x-reddit-retry", "algo=no-retries")
            .header("x-reddit-compression", "1")
            .header("x-reddit-qos", credentials.qos)
            .header("x-reddit-media-codecs", credentials.codecs)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("client-vendor-id", credentials.deviceId)
            .header("X-Reddit-Device-Id", credentials.deviceId)
            .apply {
                credentials.loid?.let { header("x-reddit-loid", it) }
                credentials.session?.let { header("x-reddit-session", it) }
            }
            .build()
    }
}
