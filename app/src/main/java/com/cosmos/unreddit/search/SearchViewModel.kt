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
import com.cosmos.unreddit.subreddit.SubredditEntity
import com.cosmos.unreddit.user.User
import com.cosmos.unreddit.util.PostUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject constructor(private val repository: PostListRepository) : ViewModel() {

    private val history: Flow<List<String>> = repository.getHistory()
        .map { list -> list.map { it.postId } }
        .distinctUntilChanged()

    private val showNsfw: Flow<Boolean> = repository.getShowNsfw()
        .distinctUntilChanged()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _query: MutableStateFlow<String?> = MutableStateFlow(null)
    val query: StateFlow<String?> get() = _query

    private val _page: MutableStateFlow<Int> = MutableStateFlow(0)
    val page: StateFlow<Int> get() = _page

    private var currentPostQuery: String? = null
    private var currentPostSorting: Sorting? = null
    private var currentPosts: Flow<PagingData<PostEntity>>? = null

    private var currentSubredditQuery: String? = null
    private var currentSubredditSorting: Sorting? = null
    private var currentSubreddits: Flow<PagingData<SubredditEntity>>? = null

    private var currentUserQuery: String? = null
    private var currentUserSorting: Sorting? = null
    private var currentUsers: Flow<PagingData<User>>? = null

    fun searchAndFilterPosts(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(searchPost(query, sorting), history, showNsfw)
            .cachedIn(viewModelScope)
    }

    private fun searchPost(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        val lastPosts = currentPosts
        if (currentPostQuery == query &&
            currentPostSorting == sorting &&
            lastPosts != null
        ) {
            return lastPosts
        }

        currentPostQuery = query
        currentPostSorting = sorting

        val newPosts = repository.searchPost(query, sorting).cachedIn(viewModelScope)
        currentPosts = newPosts

        return newPosts
    }

    fun searchAndFilterSubreddits(
        query: String,
        sorting: Sorting
    ): Flow<PagingData<SubredditEntity>> {
        return combine(searchSubreddit(query, sorting), showNsfw) { _subreddits, _showNsfw ->
            _subreddits.filter { subreddit ->
                _showNsfw || !subreddit.over18
            }
        }.cachedIn(viewModelScope)
    }

    private fun searchSubreddit(
        query: String,
        sorting: Sorting
    ): Flow<PagingData<SubredditEntity>> {
        val lastSubreddits = currentSubreddits
        if (currentSubredditQuery == query &&
            currentSubredditSorting == sorting &&
            lastSubreddits != null
        ) {
            return lastSubreddits
        }

        currentSubredditQuery = query
        currentSubredditSorting = sorting

        val newSubreddits = repository.searchSubreddit(query, sorting).cachedIn(viewModelScope)
        currentSubreddits = newSubreddits

        return newSubreddits
    }

    fun searchAndFilterUsers(
        query: String,
        sorting: Sorting
    ): Flow<PagingData<User>> {
        return combine(searchUser(query, sorting), showNsfw) { _users, _showNsfw ->
            _users.filter { user ->
                _showNsfw || !user.over18
            }
        }.cachedIn(viewModelScope)
    }

    private fun searchUser(query: String, sorting: Sorting): Flow<PagingData<User>> {
        val lastUsers = currentUsers
        if (currentUserQuery == query &&
            currentUserSorting == sorting &&
            lastUsers != null
        ) {
            return lastUsers
        }

        currentUserQuery = query
        currentUserSorting = sorting

        val newUsers = repository.searchUser(query, sorting).cachedIn(viewModelScope)
        currentUsers = newUsers

        return newUsers
    }

    fun setSorting(sorting: Sorting) {
        if (_sorting.value != sorting) {
            _sorting.value = sorting
        }
    }

    fun setQuery(query: String) {
        if (_query.value != query) {
            _query.value = query
        }
    }

    fun setPage(position: Int) {
        if (_page.value != position) {
            _page.value = position
        }
    }

    companion object {
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.RELEVANCE, RedditApi.TimeSorting.ALL)
    }
}
