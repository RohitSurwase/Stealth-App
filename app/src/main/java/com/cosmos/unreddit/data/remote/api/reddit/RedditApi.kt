package com.cosmos.unreddit.data.remote.api.reddit

import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditApi {

    //region Subreddit

    @GET("/r/{subreddit}/{sort}.json")
    suspend fun getSubreddit(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/r/{subreddit}/about.json")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String): Child

    @GET("/r/{subreddit}/search.json?restrict_sr=1&include_over_18=1")
    suspend fun searchInSubreddit(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/r/{subreddit}/about/rules.json")
    fun getSubredditRules(@Path("subreddit") subreddit: String): Call<Child>

    //endregion

    @GET("{permalink}.json")
    suspend fun getPost(
        @Path("permalink", encoded = true) permalink: String,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: Sort
    ): List<Listing>

    @GET("/api/morechildren.json?api_type=json")
    suspend fun getMoreChildren(
        @Query("children") children: String,
        @Query("link_id") linkId: String
    ): MoreChildren

    //region User

    @GET("/user/{user}/about.json")
    suspend fun getUserInfo(@Path("user") user: String): Child

    @GET("/user/{user}/submitted/.json")
    suspend fun getUserPosts(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/user/{user}/comments/.json")
    suspend fun getUserComments(
        @Path("user") user: String,
        @Query("sort") sort: Sort,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    //endregion

    //region Search

    @GET("/search.json?type=link&include_over_18=1")
    suspend fun searchPost(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/search.json?type=user&include_over_18=1")
    suspend fun searchUser(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    @GET("/search.json?type=sr&include_over_18=1")
    suspend fun searchSubreddit(
        @Query("q") query: String,
        @Query("sort") sort: Sort?,
        @Query("t") timeSorting: TimeSorting?,
        @Query("after") after: String? = null
    ): Listing

    //endregion

    enum class Sort(val type: String) {
        HOT("hot"), NEW("new"), TOP("top"), RISING("rising"),
        CONTROVERSIAL("controversial"), RELEVANCE("relevance"), COMMENTS("comments"),
        BEST("confidence"), OLD("old"), QA("qa");

        companion object {
            fun fromName(value: String?, default: Sort = BEST): Sort {
                return values().find { it.type == value } ?: default
            }
        }
    }

    enum class TimeSorting(val type: String) {
        HOUR("hour"), DAY("day"), WEEK("week"), MONTH("month"),
        YEAR("year"), ALL("all");

        companion object {
            fun fromName(value: String?): TimeSorting? {
                return values().find { it.type == value }
            }
        }
    }

    companion object {
        const val BASE_URL = "https://www.reddit.com/"
    }
}
