package com.cosmos.unreddit.util

import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.remote.api.imgur.model.Image
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object LinkUtil {

    const val USER_AGENT = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1"

    private val GIF_REGEX = Regex("gif(v)?")
    private val REDDIT_VIDEO_REGEX = Regex("DASH_(\\d+)")

    private val SUBREDDIT_REGEX = Regex("/r/[A-Za-z0-9_-]{3,21}")
    private val USER_REGEX = Regex("/u/[A-Za-z0-9_-]{3,20}")

    private val REDDIT_LINK = Regex("(.+?)\\.reddit\\.com")

    private const val REDDIT_SOUNDTRACK_NAME: String = "DASH_audio"

    fun getImageIdFromImgurLink(link: String): String {
        return link.toHttpUrlOrNull()?.pathSegments?.getOrNull(0) ?: ""
    }

    fun getAlbumIdFromImgurLink(link: String): String {
        return link.toHttpUrlOrNull()?.pathSegments?.getOrNull(1) ?: ""
    }

    fun getUrlFromImgurImage(image: Image, convertToMp4: Boolean = true): String {
        val ext = if (convertToMp4 && image.ext.contains(GIF_REGEX)) {
            ".mp4"
        } else {
            image.ext
        }
        return getUrlFromImgurId(image.hash, ext)
    }

    fun getUrlFromImgurId(hash: String, extension: String = ".jpeg"): String {
        return "https://i.imgur.com/$hash$extension"
    }

    fun getImgurVideo(link: String): String {
        return link.replace(GIF_REGEX, "mp4")
    }

    fun getRedditSoundTrack(link: String): String {
        return link.replace(REDDIT_VIDEO_REGEX, REDDIT_SOUNDTRACK_NAME)
    }

    fun isRedditSoundTrack(link: String): Boolean {
        return link.contains(REDDIT_SOUNDTRACK_NAME)
    }

    fun getGfycatId(link: String): String {
        return link.toHttpUrlOrNull()?.pathSegments?.lastOrNull() ?: return link
    }

    fun getStreamableShortcode(link: String): String {
        return link.toHttpUrlOrNull()?.pathSegments?.getOrNull(0) ?: ""
    }

    fun getLinkType(link: String): MediaType {
        when {
            link.matches(SUBREDDIT_REGEX) -> return MediaType.REDDIT_SUBREDDIT
            link.matches(USER_REGEX) -> return MediaType.REDDIT_USER
            link.startsWith("/r/") -> {
                return MediaType.REDDIT_PERMALINK
            }
        }

        val httpUrl = link.toHttpUrlOrNull() ?: return MediaType.NO_MEDIA
        val domain = httpUrl.host

        return when {
            domain.matches(REDDIT_LINK) -> {
                if (httpUrl.pathSegments.contains("wiki")) {
                    // TODO: Handle Wiki links
                    MediaType.REDDIT_WIKI
                } else if (httpUrl.pathSegments.contains("poll")) {
                    MediaType.REDDIT_POLL
                } else {
                    MediaType.REDDIT_LINK
                }
            }

            domain == "imgur.com" || domain == "m.imgur.com" -> {
                when {
                    link.contains("imgur.com/a/") -> MediaType.IMGUR_ALBUM
                    link.contains("imgur.com/gallery/") -> MediaType.IMGUR_GALLERY
                    link.endsWith(".gifv") ||
                            link.endsWith(".gif") -> MediaType.IMGUR_GIF
                    link.endsWith(".mp4") -> MediaType.IMGUR_VIDEO
                    else -> MediaType.IMGUR_LINK
                }
            }

            domain == "i.imgur.com" -> {
                when {
                    link.endsWith(".gifv") ||
                            link.endsWith(".gif") -> MediaType.IMGUR_GIF
                    link.endsWith(".mp4") -> MediaType.IMGUR_VIDEO
                    else -> MediaType.IMGUR_IMAGE
                }
            }

            domain == "www.redgifs.com" -> MediaType.REDGIFS

            domain == "streamable.com" -> MediaType.STREAMABLE

            domain == "i.redd.it" -> MediaType.IMAGE

            link.contains(".jpg") || link.contains(".jpeg") ||
                    link.contains(".png") -> MediaType.IMAGE

            link.contains(".mp4") || link.contains(".webm") -> MediaType.VIDEO

            else -> MediaType.LINK
        }
    }

    fun getPermalinkFromMediaUrl(link: String): String {
        return link.toHttpUrlOrNull()?.pathSegments?.lastOrNull() ?: link
    }
}
