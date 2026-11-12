package com.cosmos.unreddit.data.remote.api.reddit

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for a Redlib instance.
 *
 * Redlib is HTML-only (it does not expose a JSON API), so every method returns
 * the raw [ResponseBody] which is then parsed by the Redlib Jsoup scrapers.
 */
interface TedditApi {

    //region Subreddit

    @GET("/r/{subreddit}/{sort}")
    suspend fun getSubreddit(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/r/{subreddit}")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String): ResponseBody

    @GET("/r/{subreddit}/search")
    suspend fun searchInSubreddit(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("restrict_sr") restrictSubreddit: String = "on",
        @Query("after") after: String? = null
    ): ResponseBody

    //endregion

    @GET("{permalink}")
    suspend fun getPost(
        @Path("permalink", encoded = true) permalink: String,
        @Query("sort") sort: Sort
    ): ResponseBody

    //region User

    @GET("/user/{user}/submitted")
    suspend fun getUserPosts(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/user/{user}/comments")
    suspend fun getUserComments(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/user/{user}")
    suspend fun getUserInfo(@Path("user") user: String): ResponseBody

    //endregion

    //region Search

    @GET("/search?type=link")
    suspend fun searchPost(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/search?type=sr_user")
    suspend fun searchSubreddit(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    //endregion

    companion object {
        const val BASE_URL = "https://redlib.freedit.eu/"

        /**
         * Instance used until the user selects one or the online list is cached.
         */
        val DEFAULT_INSTANCE: String = BASE_URL.toHttpUrl().host
    }
}
