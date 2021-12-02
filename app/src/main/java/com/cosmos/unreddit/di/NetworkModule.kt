package com.cosmos.unreddit.di

import com.cosmos.unreddit.data.remote.RawJsonInterceptor
import com.cosmos.unreddit.data.remote.TargetRedditInterceptor
import com.cosmos.unreddit.data.remote.api.gfycat.GfycatApi
import com.cosmos.unreddit.data.remote.api.imgur.ImgurApi
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.SortingConverterFactory
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.adapter.EditedAdapter
import com.cosmos.unreddit.data.remote.api.reddit.adapter.MediaMetadataAdapter
import com.cosmos.unreddit.data.remote.api.reddit.adapter.RepliesAdapter
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.ChildType
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChild
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.streamable.StreamableApi
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

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Gfycat

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Redgifs

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class TedditOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GenericOkHttp

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

    @RedditOkHttp
    @Provides
    @Singleton
    fun provideRedditOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RawJsonInterceptor())
            .build()
    }

    @TedditOkHttp
    @Provides
    @Singleton
    fun provideTedditOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RawJsonInterceptor())
            .addInterceptor(TargetRedditInterceptor())
            .build()
    }

    @GenericOkHttp
    @Provides
    @Singleton
    fun provideGenericOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    fun provideRedditApi(
        @RedditMoshi moshi: Moshi,
        @RedditOkHttp okHttpClient: OkHttpClient
    ): RedditApi {
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
    fun provideTedditApi(
        @RedditMoshi moshi: Moshi,
        @TedditOkHttp okHttpClient: OkHttpClient
    ): TedditApi {
        return Retrofit.Builder()
            .baseUrl(TedditApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(SortingConverterFactory())
            .client(okHttpClient)
            .build()
            .create(TedditApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImgurApi(
        @BasicMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): ImgurApi {
        return Retrofit.Builder()
            .baseUrl(HttpUrl.parse(ImgurApi.BASE_URL)!!)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(ImgurApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamableApi(
        @BasicMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): StreamableApi {
        return Retrofit.Builder()
            .baseUrl(HttpUrl.parse(StreamableApi.BASE_URL)!!)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(StreamableApi::class.java)
    }

    @Gfycat
    @Provides
    @Singleton
    fun provideGfycatApi(
        @BasicMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): GfycatApi {
        return Retrofit.Builder()
            .baseUrl(GfycatApi.BASE_URL_GFYCAT)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(GfycatApi::class.java)
    }

    @Redgifs
    @Provides
    @Singleton
    fun provideRedgifsApi(
        @BasicMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): GfycatApi {
        return Retrofit.Builder()
            .baseUrl(GfycatApi.BASE_URL_REDGIFS)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(GfycatApi::class.java)
    }
}
