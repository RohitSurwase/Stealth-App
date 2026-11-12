package com.cosmos.unreddit.di

import android.content.Context
import com.cosmos.unreddit.data.remote.BrowserLikeInterceptor
import com.cosmos.unreddit.data.remote.BrowserUserAgent
import com.cosmos.unreddit.data.remote.RawJsonInterceptor
import com.cosmos.unreddit.data.remote.api.gfycat.GfycatApi
import com.cosmos.unreddit.data.remote.api.imgur.ImgurApi
import com.cosmos.unreddit.data.remote.api.imgur.adapter.AlbumDataAdapter
import com.cosmos.unreddit.data.remote.api.reddit.JsonInterceptor
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.RedditCookieJar
import com.cosmos.unreddit.data.remote.api.reddit.RedditOAuthInterceptor
import com.cosmos.unreddit.data.remote.api.reddit.RedditOAuthManager
import com.cosmos.unreddit.data.remote.api.reddit.RedditPathInterceptor
import com.cosmos.unreddit.data.remote.api.reddit.SortingConverterFactory
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.adapter.EditedAdapter
import com.cosmos.unreddit.data.remote.api.reddit.adapter.MediaMetadataAdapter
import com.cosmos.unreddit.data.remote.api.reddit.adapter.NullToEmptyStringAdapter
import com.cosmos.unreddit.data.remote.api.reddit.adapter.RepliesAdapter
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.ChildType
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChild
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.redgifs.RedgifsApi
import com.cosmos.unreddit.data.remote.api.streamable.StreamableApi
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.LinkValidator
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    private val TIMEOUT = 60L to TimeUnit.SECONDS

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditMoshi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class BasicMoshi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ImgurMoshi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class TedditOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GenericOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditScrapOkHttp

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditOfficial

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedditScrap

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RedlibBaseUrl

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
            .add(NullToEmptyStringAdapter())
            .build()
    }

    @BasicMoshi
    @Provides
    @Singleton
    fun provideBasicMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @ImgurMoshi
    @Provides
    @Singleton
    fun provideImgurMoshi(): Moshi {
        return Moshi.Builder()
            .add(AlbumDataAdapter())
            .build()
    }

    @RedditOkHttp
    @Provides
    @Singleton
    fun provideRedditOkHttpClient(
        oauthManager: RedditOAuthManager
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RawJsonInterceptor())
            .addInterceptor(JsonInterceptor())
            .addInterceptor(RedditPathInterceptor())
            .addInterceptor(RedditOAuthInterceptor(oauthManager))
            .connectTimeout(TIMEOUT.first, TIMEOUT.second)
            .readTimeout(TIMEOUT.first, TIMEOUT.second)
            .writeTimeout(TIMEOUT.first, TIMEOUT.second)
            .build()
    }

    @TedditOkHttp
    @Provides
    @Singleton
    fun provideTedditOkHttpClient(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository
    ): OkHttpClient {
        // The default User-Agent follows the installed WebView engine; users
        // can override it in Settings. Both this client and the verification
        // WebView must send the same value, otherwise cf_clearance is rejected.
        // The provider is evaluated per request, so changes apply immediately.
        val defaultUserAgent = BrowserUserAgent.fromWebView(context)
        return OkHttpClient.Builder()
            .addInterceptor(BrowserLikeInterceptor {
                val customUserAgent = runBlocking {
                    preferencesRepository.getCustomUserAgent().firstOrNull()
                }
                if (customUserAgent.isNullOrBlank()) {
                    defaultUserAgent
                } else {
                    customUserAgent
                }
            })
            .cookieJar(RedditCookieJar())
            .connectTimeout(TIMEOUT.first, TIMEOUT.second)
            .readTimeout(TIMEOUT.first, TIMEOUT.second)
            .writeTimeout(TIMEOUT.first, TIMEOUT.second)
            .build()
    }

    @GenericOkHttp
    @Provides
    @Singleton
    fun provideGenericOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT.first, TIMEOUT.second)
            .readTimeout(TIMEOUT.first, TIMEOUT.second)
            .writeTimeout(TIMEOUT.first, TIMEOUT.second)
            .build()
    }

    @RedditScrapOkHttp
    @Provides
    @Singleton
    fun provideRedditScrapOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT.first, TIMEOUT.second)
            .readTimeout(TIMEOUT.first, TIMEOUT.second)
            .writeTimeout(TIMEOUT.first, TIMEOUT.second)
            .cookieJar(RedditCookieJar())
            .build()
    }

    @RedditOfficial
    @Provides
    @Singleton
    fun provideRedditApi(
        @RedditMoshi moshi: Moshi,
        @RedditOkHttp okHttpClient: OkHttpClient
    ): RedditApi {
        return Retrofit.Builder()
            .baseUrl(RedditApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(SortingConverterFactory())
            .client(okHttpClient)
            .build()
            .create(RedditApi::class.java)
    }

    @RedditScrap
    @Provides
    @Singleton
    fun provideRedditScrapingApi(
        @RedditMoshi moshi: Moshi,
        @RedditScrapOkHttp okHttpClient: OkHttpClient
    ): RedditApi {
        return Retrofit.Builder()
            .baseUrl(RedditApi.BASE_URL_OLD)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(SortingConverterFactory())
            .client(okHttpClient)
            .build()
            .create(RedditApi::class.java)
    }

    @RedlibBaseUrl
    @Provides
    @Singleton
    fun provideRedlibBaseUrl(preferencesRepository: PreferencesRepository): HttpUrl {
        // Get the saved instance unless it's empty, then take Redlib's default instance
        val url = runBlocking {
            preferencesRepository
                .getRedditSourceInstance()
                .firstOrNull()
                .takeUnless {
                    it.isNullOrEmpty() ||
                        it.contains("teddit.net", ignoreCase = true)
                }
                ?: TedditApi.BASE_URL
        }

        return LinkValidator(url).validUrl ?: TedditApi.BASE_URL.toHttpUrl()
    }

    @Provides
    @Singleton
    fun provideTedditApi(
        @RedlibBaseUrl baseUrl: HttpUrl,
        @TedditOkHttp okHttpClient: OkHttpClient
    ): TedditApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(SortingConverterFactory())
            .client(okHttpClient)
            .build()
            .create(TedditApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImgurApi(
        @ImgurMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): ImgurApi {
        return Retrofit.Builder()
            .baseUrl(ImgurApi.BASE_URL)
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
            .baseUrl(StreamableApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(StreamableApi::class.java)
    }

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

    @Provides
    @Singleton
    fun provideRedgifsApi(
        @BasicMoshi moshi: Moshi,
        @GenericOkHttp okHttpClient: OkHttpClient
    ): RedgifsApi {
        return Retrofit.Builder()
            .baseUrl(RedgifsApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(RedgifsApi::class.java)
    }
}
