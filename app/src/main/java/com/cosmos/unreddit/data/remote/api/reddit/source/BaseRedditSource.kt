package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren

sealed interface BaseRedditSource {

    //region Subreddit
    
    suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    suspend fun getSubredditInfo(subreddit: String): Child

    suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    //endregion

    suspend fun getPost(
        permalink: String,
        limit: Int? = null,
        sort: Sort
    ): List<Listing>

    suspend fun getMoreChildren(
        children: String,
        linkId: String
    ): MoreChildren

    //region User

    suspend fun getUserInfo(user: String): Child

    suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    //endregion

    //region Search

    suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing

    //endregion
}
