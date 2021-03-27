package com.cosmos.unreddit.ui.subreddit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cosmos.unreddit.data.local.mapper.SubredditMapper
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.PagerHelper
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SubredditViewModel @Inject constructor(
    private val repository: PostListRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val history: Flow<List<String>> = repository.getHistoryIds()
        .distinctUntilChanged()

    val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _subreddit: MutableStateFlow<String?> = MutableStateFlow(null)
    val subreddit: StateFlow<String?> = _subreddit

    private val _about: MutableLiveData<Resource<SubredditEntity>> = MutableLiveData()
    val about: LiveData<Resource<SubredditEntity>> = _about

    private val _isDescriptionCollapsed = MutableLiveData(true)
    val isDescriptionCollapsed: LiveData<Boolean> get() = _isDescriptionCollapsed

    private val postPagerHelper = object : PagerHelper<PostEntity>() {
        override fun getResults(query: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
            return repository.getPosts(query, sorting).cachedIn(viewModelScope)
        }
    }

    val isSubscribed: LiveData<Boolean> = _subreddit.transform { _subreddit ->
        _subreddit?.let { subreddit ->
            repository.getSubscriptions().map { list ->
                emit(list.any { it.name.equals(subreddit, ignoreCase = true) })
            }.collect()
        }
    }.asLiveData()

    fun loadAndFilterPosts(subreddit: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(
            postPagerHelper.loadData(subreddit, sorting),
            history,
            contentPreferences
        ).cachedIn(viewModelScope)
    }

    fun loadSubredditInfo(forceUpdate: Boolean) {
        if (_subreddit.value != null) {
            if (_about.value == null || forceUpdate) {
                loadSubredditInfo(_subreddit.value!!)
            }
        } else {
            _about.value = Resource.Error()
        }
    }

    private fun loadSubredditInfo(subreddit: String) {
        viewModelScope.launch {
            repository.getSubredditInfo(subreddit).onStart {
                _about.value = Resource.Loading()
            }.catch {
                when (it) {
                    is IOException -> _about.value = Resource.Error(message = it.message)
                    is HttpException -> _about.value = Resource.Error(it.code(), it.message())
                    else -> _about.value = Resource.Error()
                }
            }.map {
                SubredditMapper.dataToEntity(it.data)
            }.collect {
                _about.value = Resource.Success(it)
            }
        }
    }

    fun setSubreddit(subreddit: String) {
        _subreddit.updateValue(subreddit)
    }

    fun setSorting(sorting: Sorting) {
        _sorting.updateValue(sorting)
    }

    fun toggleDescriptionCollapsed() {
        _isDescriptionCollapsed.value = !_isDescriptionCollapsed.value!!
    }

    fun toggleSubscription() {
        viewModelScope.launch {
            if (isSubscribed.value == true) {
                repository.unsubscribe(getSubredditName())
            } else {
                repository.subscribe(getSubredditName(), about.value?.dataValue?.icon)
            }
        }
    }

    private fun getSubredditName(): String {
        return about.value?.dataValue?.displayName ?: subreddit.value!!
    }

    companion object {
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.HOT)
    }
}
