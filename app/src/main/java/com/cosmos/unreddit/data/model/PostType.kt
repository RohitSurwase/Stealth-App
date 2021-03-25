package com.cosmos.unreddit.data.model

enum class PostType(val value: Int) {
    TEXT(0), IMAGE(1), LINK(2), VIDEO(3);

    companion object {
        fun toType(type: Int): PostType = when (type) {
            TEXT.value -> TEXT
            IMAGE.value -> IMAGE
            LINK.value -> LINK
            VIDEO.value -> VIDEO
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }
}