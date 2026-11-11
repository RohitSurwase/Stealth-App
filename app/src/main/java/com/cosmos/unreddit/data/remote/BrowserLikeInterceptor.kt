package com.cosmos.unreddit.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites the app's default OkHttp request to look like a regular browser.
 * OkHttp's default User-Agent ("okhttp/4.x") and the absence of browser headers
 * are what trigger Cloudflare's bot challenge on URLs that a real browser on the
 * same machine can open without one.
 *
 * [userAgent] must be the same string the verification WebView uses, otherwise
 * Cloudflare will reject the clearance cookie obtained there.
 */
class BrowserLikeInterceptor(
    private val userAgent: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .header("User-Agent", userAgent)
            .header("Accept", ACCEPT)
            .header("Accept-Language", "en-US,en;q=0.9")

        // Only advertise client hints that are consistent with the User-Agent.
        // A mobile Chrome UA must not be paired with desktop Sec-CH-UA values.
        if (!BrowserUserAgent.isMobile(userAgent)) {
            builder
                .header("Sec-CH-UA", SEC_CH_UA)
                .header("Sec-CH-UA-Mobile", "?0")
                .header("Sec-CH-UA-Platform", "\"Windows\"")
        }

        builder
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Dest", "document")

        return chain.proceed(builder.build())
    }

    companion object {
        private const val ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9," +
                "image/avif,image/webp,image/apng,*/*;q=0.8"

        private const val SEC_CH_UA =
            "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", " +
                "\"Google Chrome\";v=\"126\""
    }
}
