package com.cosmos.unreddit.data.remote.api.reddit

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
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

    //endregion

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

    companion object {
        const val BASE_URL = "https://teddit.net/"
    }
}
