package com.cosmos.unreddit.model

enum class MediaType {
    NO_MEDIA,

    // Reddit
    REDDIT_GALLERY, REDDIT_VIDEO,

    // Imgur
    IMGUR_GALLERY, IMGUR_ALBUM, IMGUR_IMAGE, IMGUR_VIDEO, IMGUR_GIF, IMGUR_LINK,

    // Gfycat
    GFYCAT,

    // Generic type
    IMAGE, VIDEO,

    LINK
}
