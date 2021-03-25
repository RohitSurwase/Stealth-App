package com.cosmos.unreddit.data.remote.api.reddit.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class Edited

class EditedAdapter {
    @FromJson
    @Edited
    fun fromJson(reader: JsonReader, defaultAdapter: JsonAdapter<Long>): Long {
        return if (reader.peek() === JsonReader.Token.BOOLEAN) {
            reader.skipValue()
            -1L
        } else {
            defaultAdapter.fromJson(reader)!!
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @Edited value: Long,
               defaultAdapter: JsonAdapter<Long>) {
        defaultAdapter.toJson(writer, value)
    }
}