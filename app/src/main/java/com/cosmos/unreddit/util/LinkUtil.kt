package com.cosmos.unreddit.util

import com.cosmos.unreddit.api.imgur.pojo.Image
import okhttp3.HttpUrl

object LinkUtil {

    private val GIF_REGEX = Regex("gif(v)?")
    private val REDDIT_VIDEO_REGEX = Regex("DASH_(\\d+)")

    fun getAlbumIdFromImgurLink(link: String): String {
        return HttpUrl.parse(link)?.pathSegments()?.getOrNull(1) ?: ""
    }

    fun getUrlFromImgurImage(image: Image): String {
        return "https://i.imgur.com/${image.hash}${image.ext}"
    }

    fun getImgurVideo(link: String): String {
        return link.replace(GIF_REGEX, "mp4")
    }

    fun getRedditSoundTrack(link: String): String {
        return link.replace(REDDIT_VIDEO_REGEX, "DASH_audio")
    }

    fun getGfycatVideo(link: String): String {
        return link.replace("size_restricted.gif", "mobile.mp4")
    }

    fun getStreamableShortcode(link: String): String {
        return HttpUrl.parse(link)?.pathSegments()?.getOrNull(0) ?: ""
    }
}
