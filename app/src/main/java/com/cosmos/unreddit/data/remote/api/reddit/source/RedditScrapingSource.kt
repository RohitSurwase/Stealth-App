package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.remote.api.reddit.scraper.CommentScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.PostScraper
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.DispatchersModule.MainImmediateDispatcher
import com.cosmos.unreddit.di.NetworkModule.RedditScrap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditScrapingSource @Inject constructor(
    @RedditScrap private val redditApi: RedditApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher
) : BaseRedditSource {

    private val scope = CoroutineScope(mainImmediateDispatcher + SupervisorJob())

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = redditApi.getSubreddit(subreddit, sort, timeSorting, after)
        return PostScraper(response.string(), ioDispatcher).scrap()
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        TODO("Not yet implemented")
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        val response = redditApi.getPost(permalink, limit, sort)
        val body = response.string()

        val post = scope.async {
            PostScraper(body, ioDispatcher).scrap()
        }

        val comments = scope.async {
            CommentScraper(body, ioDispatcher).scrap()
        }

        return listOf(post.await(), comments.await())
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        TODO("Not yet implemented")
    }

    override suspend fun getUserInfo(user: String): Child {
        TODO("Not yet implemented")
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        TODO("Not yet implemented")
    }
}
