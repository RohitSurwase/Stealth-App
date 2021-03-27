package com.cosmos.unreddit.data.remote.datasource

import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing

class UserPostsDataSource(
    private val redditApi: RedditApi,
    query: String,
    sorting: Sorting
) : PostListDataSource(redditApi, query, sorting) {

    override suspend fun getResponse(query: String, sorting: Sorting, after: String?): Listing {
        return redditApi.getUserPosts(query, sorting.generalSorting, sorting.timeSorting, after)
    }
}
