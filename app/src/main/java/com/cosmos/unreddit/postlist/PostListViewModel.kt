package com.cosmos.unreddit.postlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.repository.PreferencesRepository
import com.cosmos.unreddit.util.PostUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@HiltViewModel
class PostListViewModel
@Inject constructor(
    private val repository: PostListRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val history: Flow<List<String>> = repository.getHistory()
        .map { list -> list.map { it.postId } }
        .distinctUntilChanged()

    private val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    val subreddit: Flow<String> = repository.getSubscriptions()
        .map { list -> list.map { it.name } }
        .transform {
            if (it.isNotEmpty()) {
                emit(it.joinToString("+"))
            } else {
                emit(DEFAULT_SUBREDDIT)
            }
        }
        .distinctUntilChanged()

    private var currentSubreddit: String? = null
    private var currentSorting: Sorting? = null

    private var currentPosts: Flow<PagingData<PostEntity>>? = null

    fun loadAndFilterPosts(subreddit: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        return PostUtil.filterPosts(loadPosts(subreddit, sorting), history, contentPreferences)
            .cachedIn(viewModelScope)
    }

    private fun loadPosts(subreddit: String, sorting: Sorting): Flow<PagingData<PostEntity>> {
        val lastPosts = currentPosts
        if (currentSubreddit == subreddit &&
            currentSorting == sorting &&
            lastPosts != null
        ) {
            return lastPosts
        }

        currentSubreddit = subreddit
        currentSorting = sorting

        val newPosts = repository.getPosts(subreddit, sorting).cachedIn(viewModelScope)
        currentPosts = newPosts

        return newPosts
    }

    fun setSorting(sorting: Sorting) {
        if (_sorting.value != sorting) {
            _sorting.value = sorting
        }
    }

    companion object {
        private const val DEFAULT_SUBREDDIT = "popular"
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.HOT)
    }
}
