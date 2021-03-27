package com.cosmos.unreddit.util

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache

class ExoPlayerHelper(context: Context) {

    private val cacheDir = context.cacheDir

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    private val exoDatabaseProvider = ExoDatabaseProvider(context)

    private val simpleCache = SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024),
        exoDatabaseProvider
    )

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)

    val defaultMediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

    fun getMediaItem(url: String): MediaItem {
        return MediaItem.fromUri(url)
    }

    fun getMediaSource(url: String): MediaSource {
        return getMediaSource(getMediaItem(url))
    }

    fun getMediaSource(mediaItem: MediaItem): MediaSource {
        return defaultMediaSourceFactory.createMediaSource(mediaItem)
    }

    fun clearCache() {
        simpleCache.release()
        SimpleCache.delete(cacheDir, exoDatabaseProvider)
    }
}
