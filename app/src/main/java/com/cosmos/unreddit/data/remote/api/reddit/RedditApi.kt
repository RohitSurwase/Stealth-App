package com.cosmos.unreddit.data.remote.api.reddit

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditApi {

    //region Subreddit

    @GET("/r/{subreddit}/{sort}")
    suspend fun getSubreddit(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/r/{subreddit}/about")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String): ResponseBody

    @GET("/r/{subreddit}/search?restrict_sr=1&include_over_18=1")
    suspend fun searchInSubreddit(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/r/{subreddit}/about/rules")
    fun getSubredditRules(@Path("subreddit") subreddit: String): Call<Child>

    //endregion

    @GET("{permalink}")
    suspend fun getPost(
        @Path("permalink", encoded = true) permalink: String,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: Sort
    ): ResponseBody

    @GET("/api/morechildren?api_type=json")
    suspend fun getMoreChildren(
        @Query("children") children: String,
        @Query("link_id") linkId: String
    ): ResponseBody

    //region User

    @GET("/user/{user}/about")
    suspend fun getUserInfo(@Path("user") user: String): ResponseBody

    @GET("/user/{user}/submitted/")
    suspend fun getUserPosts(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/user/{user}/comments/")
    suspend fun getUserComments(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    //endregion

    //region Search

    @GET("/search?type=link&include_over_18=1")
    suspend fun searchPost(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/search?type=user&include_over_18=1")
    suspend fun searchUser(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    @GET("/search?type=sr&include_over_18=1")
    suspend fun searchSubreddit(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): ResponseBody

    //endregion

    companion object {
        const val BASE_URL = "https://www.reddit.com/"
        const val BASE_URL_OLD = "https://old.reddit.com/"
    }
}
