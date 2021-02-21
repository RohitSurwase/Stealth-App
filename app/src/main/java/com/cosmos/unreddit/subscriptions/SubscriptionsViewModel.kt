package com.cosmos.unreddit.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.subreddit.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel
@Inject constructor(repository: PostListRepository) : ViewModel() {

    private val _searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)

    val subscriptions: LiveData<List<Subscription>> =
        combine(repository.getSubscriptions(), _searchQuery) { subscriptions, searchQuery ->
            return@combine subscriptions.filter { subscription ->
                searchQuery?.let {
                    subscription.name.contains(it, ignoreCase = true)
                } ?: true
            }
        }.asLiveData()

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
        }
    }
}
