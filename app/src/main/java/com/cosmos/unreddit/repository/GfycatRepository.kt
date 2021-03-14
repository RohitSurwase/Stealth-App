package com.cosmos.unreddit.repository

import com.cosmos.unreddit.model.GalleryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class GfycatRepository @Inject constructor() {

    @Suppress("BlockingMethodInNonBlockingContext")
    fun parseRedgifsLink(
        link: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): Flow<List<GalleryMedia>> = flow {
        val document = Jsoup.connect(link).timeout(TIMEOUT).get()

        val videoUrl = document.selectFirst("video")
            ?.selectFirst("source")
            ?.attr("src")

        if (videoUrl != null) {
            emit(GalleryMedia.singleton(GalleryMedia.Type.VIDEO, videoUrl))
        }
    }.flowOn(coroutineContext)

    companion object {
        private const val TIMEOUT = 10000
    }
}
