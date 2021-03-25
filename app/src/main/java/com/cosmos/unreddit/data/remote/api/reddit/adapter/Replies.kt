package com.cosmos.unreddit.data.remote.api.reddit.adapter

import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class Replies

class RepliesAdapter {
    @FromJson
    @Replies
    fun fromJson(reader: JsonReader, defaultAdapter: JsonAdapter<Listing>): Listing? {
        return if (reader.peek() === JsonReader.Token.STRING) {
            reader.skipValue()
            null
        } else {
            defaultAdapter.fromJson(reader)
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @Replies value: Listing,
               defaultAdapter: JsonAdapter<Listing>) {
        defaultAdapter.toJson(writer, value)
    }
}