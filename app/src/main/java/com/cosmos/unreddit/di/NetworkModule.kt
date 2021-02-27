package com.cosmos.unreddit.di

import com.cosmos.unreddit.api.EditedAdapter
import com.cosmos.unreddit.api.RawJsonInterceptor
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.api.RepliesAdapter
import com.cosmos.unreddit.api.SortingConverterFactory
import com.cosmos.unreddit.api.adapter.MediaMetadataAdapter
import com.cosmos.unreddit.api.imgur.ImgurApi
import com.cosmos.unreddit.api.pojo.details.AboutChild
import com.cosmos.unreddit.api.pojo.details.AboutUserChild
import com.cosmos.unreddit.api.pojo.details.Child
import com.cosmos.unreddit.api.pojo.details.ChildType
import com.cosmos.unreddit.api.pojo.details.CommentChild
import com.cosmos.unreddit.api.pojo.details.MoreChild
import com.cosmos.unreddit.api.pojo.details.PostChild
import com.cosmos.unreddit.api.streamable.StreamableApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditMoshi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class BasicMoshi

    @RedditMoshi
    @Provides
    @Singleton
    fun provideRedditMoshi(): Moshi {
        return Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(Child::class.java, "kind")
                .withSubtype(CommentChild::class.java, ChildType.t1.name)
                .withSubtype(AboutUserChild::class.java, ChildType.t2.name)
                .withSubtype(PostChild::class.java, ChildType.t3.name)
                .withSubtype(AboutChild::class.java, ChildType.t5.name)
                .withSubtype(MoreChild::class.java, ChildType.more.name))
            .add(MediaMetadataAdapter.Factory)
            .add(RepliesAdapter())
            .add(EditedAdapter())
            .build()
    }

    @BasicMoshi
    @Provides
    @Singleton
    fun provideBasicMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RawJsonInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRedditApi(@RedditMoshi moshi: Moshi, okHttpClient: OkHttpClient): RedditApi {
        return Retrofit.Builder()
            .baseUrl(HttpUrl.parse(RedditApi.BASE_URL)!!)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(SortingConverterFactory())
            .client(okHttpClient)
            .build()
            .create(RedditApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImgurApi(@BasicMoshi moshi: Moshi, okHttpClient: OkHttpClient): ImgurApi {
        return Retrofit.Builder()
            .baseUrl(HttpUrl.parse(ImgurApi.BASE_URL)!!)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(ImgurApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamableApi(@BasicMoshi moshi: Moshi, okHttpClient: OkHttpClient): StreamableApi {
        return Retrofit.Builder()
            .baseUrl(HttpUrl.parse(StreamableApi.BASE_URL)!!)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(StreamableApi::class.java)
    }
}
