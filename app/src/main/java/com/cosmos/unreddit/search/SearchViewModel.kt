package com.cosmos.unreddit.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.repository.PreferencesRepository
import com.cosmos.unreddit.subreddit.SubredditEntity
import com.cosmos.unreddit.user.User
import com.cosmos.unreddit.util.PagerHelper
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PostListRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val history: Flow<List<String>> = repository.getHistoryIds().distinctUntilChanged()

    val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _query: MutableStateFlow<String?> = MutableStateFlow(null)
    val query: StateFlow<String?> get() = _query

    private val _page: MutableStateFlow<Int> = MutableStateFlow(0)
    val page: StateFlow<Int> get() = _page

    private val postPagerHelper = object : PagerHelper<PostEntity>() {
        override fun getResults(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
            return repository.searchPost(query, sorting).cachedIn(viewModelScope)
        }
    }

    private val subredditPagerHelper = object : PagerHelper<SubredditEntity>() {
        override fun getResults(
            query: String,
            sorting: Sorting
        ): Flow<PagingData<SubredditEntity>> {
            return repository.searchSubreddit(query, sorting).cachedIn(viewModelScope)
        }
    }

    private val userPagerHelper = object : PagerHelper<User>() {
        override fun getResults(query: String, sorting: Sorting): Flow<PagingData<User>> {
            return repository.searchUser(query, sorting).cachedIn(viewModelScope)
        }
    }

    fun searchAndFilterPosts(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(
            postPagerHelper.loadData(query, sorting),
            history,
            contentPreferences
        ).cachedIn(viewModelScope)
    }

    fun searchAndFilterSubreddits(
        query: String,
        sorting: Sorting
    ): Flow<PagingData<SubredditEntity>> {
        return combine(
            subredditPagerHelper.loadData(query, sorting),
            contentPreferences
        ) { _subreddits, _contentPreferences ->
            _subreddits.filter { subreddit ->
                _contentPreferences.showNsfw || !subreddit.over18
            }
        }.cachedIn(viewModelScope)
    }

    fun searchAndFilterUsers(
        query: String,
        sorting: Sorting
    ): Flow<PagingData<User>> {
        return combine(
            userPagerHelper.loadData(query, sorting),
            contentPreferences
        ) { _users, _contentPreferences ->
            _users.filter { user ->
                _contentPreferences.showNsfw || !user.over18
            }
        }.cachedIn(viewModelScope)
    }

    fun setSorting(sorting: Sorting) {
        _sorting.updateValue(sorting)
    }

    fun setQuery(query: String) {
        _query.updateValue(query)
    }

    fun setPage(position: Int) {
        _page.updateValue(position)
    }

    companion object {
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.RELEVANCE, RedditApi.TimeSorting.ALL)
    }
}
