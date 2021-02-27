package com.cosmos.unreddit.util

import com.cosmos.unreddit.api.imgur.pojo.Image
import okhttp3.HttpUrl

object LinkUtil {

    fun getAlbumIdFromImgurLink(link: String): String {
        return HttpUrl.parse(link)?.pathSegments()?.getOrNull(1) ?: ""
    }

    fun getUrlFromImgurImage(image: Image): String {
        return "https://i.imgur.com/${image.hash}${image.ext}"
    }
}
