package com.cosmos.unreddit.ui.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    repository: PostListRepository
) : BaseViewModel(preferencesRepository, repository) {

    private val _searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)

    val filteredSubscriptions: LiveData<List<Subscription>> = combine(
        subscriptions,
        _searchQuery
    ) { subscriptions, searchQuery ->
        subscriptions.filter { subscription ->
            searchQuery?.let { query ->
                subscription.name.contains(query, ignoreCase = true)
            } ?: true
        }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
        }
    }
}
