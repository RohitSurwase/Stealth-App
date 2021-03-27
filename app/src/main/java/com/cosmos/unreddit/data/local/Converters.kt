package com.cosmos.unreddit.data.local

import androidx.room.TypeConverter
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.PosterType

class Converters {
    @TypeConverter
    fun fromPostTypeInt(type: Int?): PostType? {
        return type?.let { PostType.toType(it) }
    }

    @TypeConverter
    fun toPostTypeInt(postType: PostType?): Int? {
        return postType?.value
    }

    @TypeConverter
    fun fromPosterTypeInt(type: Int?): PosterType? {
        return type?.let { PosterType.toType(it) }
    }

    @TypeConverter
    fun toPosterTypeInt(posterType: PosterType?): Int? {
        return posterType?.value
    }
}
