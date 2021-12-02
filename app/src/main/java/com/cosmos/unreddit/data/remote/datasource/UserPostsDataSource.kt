package com.cosmos.unreddit.data.remote.datasource

import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource

class UserPostsDataSource(
    private val source: CurrentSource,
    query: String,
    sorting: Sorting
) : PostListDataSource(source, query, sorting) {

    override suspend fun getResponse(query: String, sorting: Sorting, after: String?): Listing {
        return source.getUserPosts(query, sorting.generalSorting, sorting.timeSorting, after)
    }
}
