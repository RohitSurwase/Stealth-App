package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TedditSource @Inject constructor(private val tedditApi: TedditApi) : BaseRedditSource {

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return tedditApi.getSubreddit(subreddit, sort, timeSorting, after)
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun getUserInfo(user: String): Child {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return tedditApi.getUserPosts(user, sort, timeSorting, after)
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return tedditApi.getUserComments(user, sort, timeSorting, after)
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        throw UnsupportedOperationException("No API endpoint")
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        throw UnsupportedOperationException("No API endpoint")
    }
}
