package com.cosmos.unreddit.util

import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.io.InputStream

/**
 * ExoPlayer [DataSource] that fetches bytes through an injected OkHttp client instead of
 * the platform [java.net.HttpURLConnection] that ExoPlayer's built-in [DefaultHttpDataSource]
 * uses.
 *
 * redlib serves HLS behind a Cloudflare / Anubis bot challenge bound to the browser request
 * fingerprint — a WebView-identical mobile-Chrome User-Agent plus browser request headers
 * (Accept / Accept-Language / Sec-Fetch-*) and the `cf_clearance` clearance cookie. ExoPlayer's
 * built-in DefaultHttpDataSource sends only LinkUtil.USER_AGENT (a bare Firefox desktop UA) with
 * none of those headers and no clearance cookie, so Cloudflare rejects it with 403. The app's
 * browser-like OkHttp client ([BrowserLikeInterceptor] +
 * [com.cosmos.unreddit.data.remote.api.reddit.RedditCookieJar] + browser UA) is byte-identical to
 * the verification WebView, so routing the m3u8 playlist and the .ts/.m4s segments through it
 * presents the same request fingerprint + cookie that passed the challenge.
 *
 * NOTE: [BrowserLikeInterceptor] rewrites only HTTP headers, not TLS, so this client and
 * ExoPlayer's platform TLS stack share the same JA3. The challenge is therefore NOT JA3/TLS-bound
 * (otherwise the browser-like client would also fail on the feed, which it does not); it is bound
 * to the User-Agent + request headers + clearance cookie, which this DataSource now supplies.
 */
class OkHttpDataSource(
    private val okHttpClient: OkHttpClient,
) : DataSource {

    private val TAG = "OkHttpDataSource"

    private var uri: Uri? = null
    private var inputStream: InputStream? = null

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val httpUrl = dataSpec.uri.toString().toHttpUrlOrNull()
            ?: throw IOException("Invalid URL: ${dataSpec.uri}")

        val requestBuilder = Request.Builder().url(httpUrl)
        // Honor headers ExoPlayer set (e.g. Range for partial segment reads).
        dataSpec.httpRequestHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }
        // Honor DataSpec position/length (HLS byte-range segments address a single .mp4 file).
        // Only apply if the caller hasn't set a Range header explicitly.
        if (requestBuilder.build().header("Range") == null) {
            val rangeHeader = when {
                dataSpec.length != C.LENGTH_UNSET.toLong() ->
                    "bytes=${dataSpec.position}-${dataSpec.position + dataSpec.length - 1}"
                dataSpec.position > 0 -> "bytes=${dataSpec.position}-"
                else -> null
            }
            rangeHeader?.let { requestBuilder.header("Range", it) }
        }

        val response = okHttpClient.newCall(requestBuilder.get().build()).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "redlib HLS request rejected: code=${response.code} ct=${response.header("Content-Type")} url=$httpUrl")
            response.close()
            throw HttpDataSource.InvalidResponseCodeException(
                response.code,
                "OkHttp response code: ${response.code}",
                null,
                emptyMap(),
                dataSpec,
                ByteArray(0)
            )
        }

        val body = response.body
            ?: run {
                response.close()
                throw IOException("Empty response body for $httpUrl")
            }
        inputStream = body.byteStream()
        // ExoPlayer contract: unknown length must be C.LENGTH_UNSET, not 0.
        return body.contentLength().takeIf { it >= 0 } ?: C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val stream = inputStream ?: throw IOException("DataSource is not open")
        val read = stream.read(buffer, offset, length)
        // ExoPlayer's DataSource contract: signal end-of-input by returning C.RESULT_END_OF_INPUT,
        // never by throwing EOFException (that aborts parsing mid-stream).
        return if (read == -1) C.RESULT_END_OF_INPUT else read
    }

    override fun close() {
        inputStream?.close()
        inputStream = null
    }

    override fun getUri(): Uri? = uri

    override fun addTransferListener(transferListener: TransferListener) {
        // No per-listener tracking needed for a pass-through fetch.
    }
}

/** [DataSource.Factory] that produces [OkHttpDataSource] instances backed by [okHttpClient]. */
class OkHttpDataSourceFactory(
    private val okHttpClient: OkHttpClient,
) : DataSource.Factory {

    override fun createDataSource(): DataSource = OkHttpDataSource(okHttpClient)
}
