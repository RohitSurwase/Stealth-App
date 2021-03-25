package com.cosmos.unreddit.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cosmos.unreddit.data.local.RedditDatabase
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.model.db.History
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.remote.datasource.CommentsDataSource
import com.cosmos.unreddit.data.remote.datasource.PostListDataSource
import com.cosmos.unreddit.data.remote.datasource.SearchPostDataSource
import com.cosmos.unreddit.data.remote.datasource.SearchSubredditDataSource
import com.cosmos.unreddit.data.remote.datasource.SearchUserDataSource
import com.cosmos.unreddit.data.remote.datasource.SubredditSearchPostDataSource
import com.cosmos.unreddit.data.remote.datasource.UserPostsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostListRepository @Inject constructor(
    private val redditApi: RedditApi,
    private val redditDatabase: RedditDatabase
) {

    fun getPost(permalink: String, sorting: Sorting): Flow<List<Listing>> = flow {
        emit(redditApi.getPost(permalink, sort = sorting.generalSorting))
    }

    fun getMoreChildren(children: String, linkId: String): Flow<MoreChildren> = flow {
        emit(redditApi.getMoreChildren(children, linkId))
    }

    //region Subreddit

    fun getPosts(
        subreddit: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<PostEntity>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            PostListDataSource(redditApi, subreddit, sorting)
        }.flow
    }

    fun getSubredditInfo(subreddit: String): Flow<AboutChild> = flow {
        emit(redditApi.getSubredditInfo(subreddit) as AboutChild)
    }

    //endregion

    //region Subscriptions

    fun getSubscriptions(): Flow<List<Subscription>> = redditDatabase.subscriptionDao()
        .getSubscriptions().distinctUntilChanged()

    suspend fun subscribe(name: String, icon: String? = null) {
        redditDatabase.subscriptionDao().insert(Subscription(name, System.currentTimeMillis(), icon))
    }

    suspend fun unsubscribe(name: String) {
        redditDatabase.subscriptionDao().deleteFromName(name)
    }

    //endregion

    //region User

    fun getUserPosts(
        user: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<PostEntity>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            UserPostsDataSource(redditApi, user, sorting)
        }.flow
    }

    fun getUserComments(
        user: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<Comment>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            CommentsDataSource(redditApi, user, sorting)
        }.flow
    }

    fun getUserInfo(user: String): Flow<AboutUserChild> = flow {
        emit(redditApi.getUserInfo(user) as AboutUserChild)
    }

    //endregion

    //region Search

    fun searchPost(
        query: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<PostEntity>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            SearchPostDataSource(redditApi, query, sorting)
        }.flow
    }

    fun searchUser(
        query: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<User>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            SearchUserDataSource(redditApi, query, sorting)
        }.flow
    }

    fun searchSubreddit(
        query: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<SubredditEntity>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            SearchSubredditDataSource(redditApi, query, sorting)
        }.flow
    }

    fun searchInSubreddit(
        query: String,
        subreddit: String,
        sorting: Sorting,
        pageSize: Int = DEFAULT_LIMIT
    ): Flow<PagingData<PostEntity>> {
        return Pager(PagingConfig(pageSize = pageSize)) {
            SubredditSearchPostDataSource(redditApi, subreddit, query, sorting)
        }.flow
    }

    //endregion

    fun getHistory(): Flow<List<History>> {
        return redditDatabase.historyDao().getHistory()
    }

    fun getHistoryIds(): Flow<List<String>> {
        return redditDatabase.historyDao().getHistory()
            .map { list -> list.map { it.postId } }
    }

    suspend fun insertPostInHistory(id: String) {
        redditDatabase.historyDao().upsert(History(id, System.currentTimeMillis()))
    }

    companion object {
        private const val DEFAULT_LIMIT = 25
    }
}
