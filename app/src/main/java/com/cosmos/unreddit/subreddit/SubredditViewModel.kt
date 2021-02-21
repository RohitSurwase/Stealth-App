package com.cosmos.unreddit.subreddit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.database.SubredditMapper
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.util.PagerHelper
import com.cosmos.unreddit.util.PostUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubredditViewModel
@Inject constructor(private val repository: PostListRepository) : ViewModel() {

    private val history: Flow<List<String>> = repository.getHistoryIds()
        .distinctUntilChanged()

    private val showNsfw: Flow<Boolean> = repository.getShowNsfw()
        .distinctUntilChanged()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _subreddit: MutableStateFlow<String?> = MutableStateFlow(null)
    val subreddit: StateFlow<String?> = _subreddit

    private val _isDescriptionCollapsed = MutableLiveData(true)
    val isDescriptionCollapsed: LiveData<Boolean> get() = _isDescriptionCollapsed

    private val postPagerHelper = object : PagerHelper<PostEntity>() {
        override fun getResults(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
            return repository.getPosts(query, sorting).cachedIn(viewModelScope)
        }
    }

    val about: LiveData<SubredditEntity> = _subreddit.transform { _subreddit ->
        _subreddit?.let { subreddit ->
            repository.getSubredditInfo(subreddit).map { subredditInfo ->
                emit(SubredditMapper.dataToEntity(subredditInfo.data))
            }.collect()
        }
    }.asLiveData()

    val isSubscribed: LiveData<Boolean> = _subreddit.transform { _subreddit ->
        _subreddit?.let { subreddit ->
            repository.getSubscriptions().map { list ->
                emit(list.any { it.name.equals(subreddit, ignoreCase = true) })
            }.collect()
        }
    }.asLiveData()

    fun loadAndFilterPosts(subreddit: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(postPagerHelper.loadData(subreddit, sorting), history, showNsfw)
            .cachedIn(viewModelScope)
    }

    fun setSubreddit(subreddit: String) {
        if (_subreddit.value != subreddit) {
            _subreddit.value = subreddit
        }
    }

    fun setSorting(sorting: Sorting) {
        if (_sorting.value != sorting) {
            _sorting.value = sorting
        }
    }

    fun toggleDescriptionCollapsed() {
        _isDescriptionCollapsed.value = !_isDescriptionCollapsed.value!!
    }

    fun toggleSubscription() {
        viewModelScope.launch {
            if (isSubscribed.value == true) {
                repository.unsubscribe(getSubredditName())
            } else {
                repository.subscribe(getSubredditName(), about.value?.icon)
            }
        }
    }

    private fun getSubredditName(): String {
        return about.value?.displayName ?: subreddit.value!!
    }

    companion object {
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.HOT)
    }
}
