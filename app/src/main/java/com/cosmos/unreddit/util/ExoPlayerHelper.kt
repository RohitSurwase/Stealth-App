package com.cosmos.unreddit.util

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import okhttp3.OkHttpClient
import kotlin.getValue

/**
 * Bail out immediately (no silent retries) when a redlib HLS load is rejected by the bot
 * challenge (403/429), so the media viewer can surface the inline verification flow quickly
 * instead of the player retrying the challenge for ~50 s behind the scenes.
 */
private object RedlibLoadErrorPolicy : com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val cause = loadErrorInfo.exception as? HttpDataSource.InvalidResponseCodeException
        if (cause != null &&
            (cause.responseCode == java.net.HttpURLConnection.HTTP_FORBIDDEN || cause.responseCode == 429)
        ) {
            return com.google.android.exoplayer2.C.TIME_UNSET
        }
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}

class ExoPlayerHelper(
    context: Context,
    /**
     * The app's browser-like OkHttp client (BrowserLikeInterceptor + RedditCookieJar + browser
     * User-Agent). Used only for redlib HLS, which sits behind a Cloudflare/Anubis bot challenge
     * bound to the browser User-Agent + request headers + clearance cookie.
     */
    private val redlibOkHttpClient: OkHttpClient,
) {

    private val cacheDir = context.cacheDir

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent(LinkUtil.USER_AGENT)

    private val exoDatabaseProvider = ExoDatabaseProvider(context)

    private val simpleCache = SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024),
        exoDatabaseProvider
    )

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)

    val defaultMediaSourceFactory by lazy { DefaultMediaSourceFactory(cacheDataSourceFactory) }

    /**
     * Media source factory for redlib HLS. Fetches the m3u8 playlist and every segment through
     * the browser-like OkHttp client so the request carries the browser User-Agent + request
     * headers + clearance cookie that passed the Cloudflare/Anubis challenge. A plain (uncached)
     * factory is used because the playlist URL carries a per-load token, so caching would only risk
     * stale reads.
     */
    val redlibMediaSourceFactory by lazy {
        DefaultMediaSourceFactory(OkHttpDataSourceFactory(redlibOkHttpClient))
            .setLoadErrorHandlingPolicy(RedlibLoadErrorPolicy)
    }

    fun setRequestProperties(requestProperties: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(requestProperties)
    }

    fun getMediaItem(url: String): MediaItem {
        return MediaItem.fromUri(url)
    }

    fun getMediaSource(url: String): MediaSource {
        return getMediaSource(getMediaItem(url))
    }

    fun getMediaSource(
        mediaItem: MediaItem,
        factory: DefaultMediaSourceFactory = defaultMediaSourceFactory,
    ): MediaSource {
        return factory.createMediaSource(mediaItem)
    }

    /** Whether [dataSourceFactory] should route through the browser-like OkHttp client. */
    fun isRedlibFactory(factory: DataSource.Factory): Boolean =
        factory is OkHttpDataSourceFactory

    fun clearCache() {
        simpleCache.release()
        SimpleCache.delete(cacheDir, exoDatabaseProvider)
    }
}
