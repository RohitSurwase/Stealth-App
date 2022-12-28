package com.cosmos.unreddit.data.remote.api.imgur.adapter

import com.cosmos.unreddit.data.remote.api.imgur.model.Data
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class AlbumData

class AlbumDataAdapter {
    @FromJson
    @AlbumData
    fun fromJson(reader: JsonReader, defaultAdapter: JsonAdapter<Data>): Data {
        return if (reader.peek() === JsonReader.Token.BEGIN_ARRAY) {
            reader.skipValue()
            Data(0, listOf())
        } else {
            defaultAdapter.fromJson(reader)!!
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @AlbumData value: Data, defaultAdapter: JsonAdapter<Data>) {
        defaultAdapter.toJson(writer, value)
    }
}
