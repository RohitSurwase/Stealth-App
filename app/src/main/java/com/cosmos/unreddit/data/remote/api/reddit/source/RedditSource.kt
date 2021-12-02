package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditSource @Inject constructor(private val redditApi: RedditApi) : BaseRedditSource {

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.getSubreddit(subreddit, sort, timeSorting, after)
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        return redditApi.getSubredditInfo(subreddit)
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.searchInSubreddit(subreddit, query, sort, timeSorting, after)
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        return redditApi.getPost(permalink, limit, sort)
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        return redditApi.getMoreChildren(children, linkId)
    }

    override suspend fun getUserInfo(user: String): Child {
        return redditApi.getUserInfo(user)
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.getUserPosts(user, sort, timeSorting, after)
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.getUserComments(user, sort, timeSorting, after)
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.searchPost(query, sort, timeSorting, after)
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.searchUser(query, sort, timeSorting, after)
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return redditApi.searchSubreddit(query, sort, timeSorting, after)
    }
}
