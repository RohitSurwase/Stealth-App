package com.cosmos.unreddit.ui.postlist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.PagerHelper
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.RedditUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PostListViewModel
@Inject constructor(
    private val repository: PostListRepository,
    preferencesRepository: PreferencesRepository
) : BaseViewModel(preferencesRepository, repository) {

    val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    val subreddit: Flow<String> = subscriptionsNames.map {
        if (it.isNotEmpty()) {
            RedditUtil.joinSubredditList(it)
        } else {
            DEFAULT_SUBREDDIT
        }
    }.distinctUntilChanged()

    private val postPagerHelper = object : PagerHelper<PostEntity>() {
        override fun getResults(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
            return repository.getPosts(query, sorting).cachedIn(viewModelScope)
        }
    }

    fun loadAndFilterPosts(subreddit: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(
            postPagerHelper.loadData(subreddit, sorting),
            historyIds,
            savedPostIds,
            contentPreferences
        ).cachedIn(viewModelScope)
    }

    fun setSorting(sorting: Sorting) {
        _sorting.updateValue(sorting)
    }

    companion object {
        private const val DEFAULT_SUBREDDIT = "popular"
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.HOT)
    }
}
