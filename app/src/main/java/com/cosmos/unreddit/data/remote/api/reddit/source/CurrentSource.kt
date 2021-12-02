package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.CoroutinesScopesModule.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentSource @Inject constructor(
    @ApplicationScope externalScope: CoroutineScope,
    private val preferencesRepository: PreferencesRepository,
    private val redditSource: RedditSource,
    private val tedditSource: TedditSource
) : BaseRedditSource {

    private val mutex = Mutex()

    private var source: BaseRedditSource = runBlocking {
        val sourceValue = preferencesRepository.getRedditSource().first()
        getRedditSource(sourceValue)
    }

    init {
        externalScope.launch {
            preferencesRepository.getRedditSource()
                .drop(1) // Drop first value to avoid conflicts
                .collect { sourceValue ->
                    mutex.withLock {
                        source = getRedditSource(sourceValue)
                    }
                }
        }
    }

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return source.getSubreddit(subreddit, sort, timeSorting, after)
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.getSubredditInfo(subreddit)
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.searchInSubreddit(subreddit, query, sort, timeSorting, after)
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.getPost(permalink, limit, sort)
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.getMoreChildren(children, linkId)
    }

    override suspend fun getUserInfo(user: String): Child {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.getUserInfo(user)
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return source.getUserPosts(user, sort, timeSorting, after)
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return source.getUserComments(user, sort, timeSorting, after)
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.searchPost(query, sort, timeSorting, after)
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.searchUser(query, sort, timeSorting, after)
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO: Replace by source when an endpoint is available for Teddit
        return redditSource.searchSubreddit(query, sort, timeSorting, after)
    }

    private fun getRedditSource(value: Int): BaseRedditSource {
        return when(BaseRedditSource.Source.fromValue(value)) {
            BaseRedditSource.Source.REDDIT -> redditSource
            BaseRedditSource.Source.TEDDIT -> tedditSource
        }
    }
}
