package com.cosmos.unreddit.data.model

import androidx.annotation.Keep

@Keep
enum class MediaType {
    NO_MEDIA,

    // Reddit
    REDDIT_GALLERY, REDDIT_VIDEO, REDDIT_GIF,
    REDDIT_SUBREDDIT, REDDIT_USER, REDDIT_LINK,

    // Imgur
    IMGUR_GALLERY, IMGUR_ALBUM, IMGUR_IMAGE, IMGUR_VIDEO, IMGUR_GIF, IMGUR_LINK,

    // Gfycat
    GFYCAT, REDGIFS,

    // Streamable
    STREAMABLE,

    // Generic type
    IMAGE, VIDEO,

    LINK
}
