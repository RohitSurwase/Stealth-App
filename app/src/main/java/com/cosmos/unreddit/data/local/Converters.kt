package com.cosmos.unreddit.data.local

import androidx.room.TypeConverter
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.model.db.Redirect

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

    @TypeConverter
    fun fromSortingString(string: String?): Sorting? {
        return string?.let {
            val values = it.split(";")
            Sorting(
                Sort.fromName(values.getOrNull(0)),
                TimeSorting.fromName(values.getOrNull(1))
            )
        }
    }

    @TypeConverter
    fun toSortingString(sorting: Sorting?): String? {
        return sorting?.let {
            listOfNotNull(it.generalSorting.name, it.timeSorting?.name).joinToString(";")
        }
    }

    @TypeConverter
    fun fromRedirectModeInt(mode: Int?): Redirect.RedirectMode {
        return mode?.let { Redirect.RedirectMode.toMode(it) } ?: Redirect.RedirectMode.OFF
    }

    @TypeConverter
    fun toRedirectModeInt(redirectMode: Redirect.RedirectMode?): Int {
        return redirectMode?.mode ?: Redirect.RedirectMode.OFF.mode
    }
}
