package com.cosmos.unreddit.data.remote.api.reddit

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class SortingConverterFactory : Converter.Factory() {

    override fun stringConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        return when (type) {
            RedditApi.Sort::class.java -> {
                Converter<RedditApi.Sort, String> { it.type }
            }
            RedditApi.TimeSorting::class.java -> {
                Converter<RedditApi.TimeSorting, String> { it.type }
            }
            else -> {
                super.stringConverter(type, annotations, retrofit)
            }
        }
    }
}
