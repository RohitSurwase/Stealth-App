package com.cosmos.unreddit.data.remote.api.reddit.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class NullToEmptyString

class NullToEmptyStringAdapter {
    @FromJson
    @NullToEmptyString
    fun fromJson(reader: JsonReader, defaultAdapter: JsonAdapter<String>): String {
        return defaultAdapter.fromJson(reader) ?: ""
    }

    @ToJson
    fun toJson(
        writer: JsonWriter,
        @NullToEmptyString value: String,
        defaultAdapter: JsonAdapter<String>
    ) {
        defaultAdapter.toJson(writer, value)
    }
}
