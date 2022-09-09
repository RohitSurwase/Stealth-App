package com.cosmos.unreddit.data.repository

import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.remote.api.gfycat.GfycatApi
import com.cosmos.unreddit.data.remote.api.gfycat.model.Item
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.NetworkModule.Gfycat
import com.cosmos.unreddit.di.NetworkModule.Redgifs
import com.cosmos.unreddit.util.LinkUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GfycatRepository @Inject constructor(
    @Gfycat private val gfycatApi: GfycatApi,
    @Redgifs private val redgifsApi: GfycatApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun getGfycatGif(id: String): Flow<Item> = flow {
        emit(gfycatApi.getGif(id))
    }

    fun getRedgifsGif(id: String): Flow<Item> = flow {
        emit(redgifsApi.getGif(id))
    }

    fun parseRedgifsLink(link: String): Flow<List<GalleryMedia>> = flow {
        val document = Jsoup
            .connect(link)
            .userAgent(LinkUtil.USER_AGENT)
            .header("User-Agent", LinkUtil.USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9," +
                    "image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("DNT", "1")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("TE", "trailers")
            .timeout(TIMEOUT)
            .get()

        val videoUrl = document.select("[property=og:video]")
            .eachAttr("content")
            .firstOrNull()

        if (videoUrl != null) {
            emit(GalleryMedia.singleton(GalleryMedia.Type.VIDEO, videoUrl))
        } else {
            throw Exception("Video URL is null")
        }
    }.flowOn(ioDispatcher)

    companion object {
        private const val TIMEOUT = 10000
    }
}
