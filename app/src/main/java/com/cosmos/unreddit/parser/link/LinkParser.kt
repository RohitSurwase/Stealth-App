package com.cosmos.unreddit.parser.link

import com.cosmos.unreddit.model.GalleryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException

object LinkParser {

    private const val TIMEOUT = 10000

    suspend fun parseRedgifsLink(link: String): List<GalleryMedia> {
        return withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val document = Jsoup.connect(link).timeout(TIMEOUT).get()

                val videoUrl = document.selectFirst("video")
                    ?.selectFirst("source")
                    ?.attr("src")

                if (videoUrl != null) {
                    return@withContext GalleryMedia.singleton(GalleryMedia.Type.VIDEO, videoUrl)
                }
            }.onFailure {
                when (it) {
                    is MalformedURLException,
                    is HttpStatusException,
                    is UnsupportedMimeTypeException,
                    is SocketTimeoutException,
                    is IOException -> {
                        // ignore
                    }
                    else -> throw it
                }
            }

            listOf()
        }
    }
}
