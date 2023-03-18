package com.cosmos.unreddit.data.remote.api.reddit

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.TedditUser
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TedditApi {

    //region Subreddit

    @GET("/r/{subreddit}/{sort}?api")
    suspend fun getSubreddit(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/r/{subreddit}/about?api")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String): Child

    @GET("/r/{subreddit}/search?api&restrict_sr=on&nsfw=on")
    suspend fun searchInSubreddit(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    //endregion

    @GET("{permalink}?api")
    suspend fun getPost(
        @Path("permalink", encoded = true) permalink: String,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: Sort
    ): List<Listing>

    //region User

    @GET("/u/{user}/submitted?api")
    suspend fun getUserPosts(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): TedditUser

    @GET("/u/{user}/comments?api")
    suspend fun getUserComments(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): TedditUser

    //endregion

    //region Search

    @GET("/subreddits/search?api&nsfw=on")
    suspend fun searchSubreddit(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    //endregion

    companion object {
        const val BASE_URL = "https://teddit.net/"
    }
}
