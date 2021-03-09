package com.cosmos.unreddit.api.adapter

import com.cosmos.unreddit.api.pojo.GalleryItem
import com.cosmos.unreddit.api.pojo.MediaMetadata
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

class MediaMetadataAdapter(
    moshi: Moshi
) : JsonAdapter<MediaMetadata>() {

    private val galleryItemAdapter: JsonAdapter<GalleryItem> =
        moshi.adapter(GalleryItem::class.java, emptySet())

    override fun fromJson(reader: JsonReader): MediaMetadata? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.skipValue()
            return null
        }

        val items = mutableListOf<GalleryItem>()

        reader.beginObject()

        while (reader.hasNext()) {
            reader.skipName()
            items.add(galleryItemAdapter.fromJson(reader)!!)
        }

        reader.endObject()

        return MediaMetadata(items)
    }

    override fun toJson(writer: JsonWriter, value: MediaMetadata?) {
    }

    object Factory : JsonAdapter.Factory {
        override fun create(
            type: Type,
            annotations: MutableSet<out Annotation>,
            moshi: Moshi
        ): JsonAdapter<*>? {
            if (annotations.isNotEmpty()) return null
            if (Types.getRawType(type) == MediaMetadata::class.java) {
                return MediaMetadataAdapter(moshi)
            }
            return null
        }
    }
}
