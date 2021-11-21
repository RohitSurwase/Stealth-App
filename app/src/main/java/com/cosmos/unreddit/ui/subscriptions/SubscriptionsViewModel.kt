package com.cosmos.unreddit.ui.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    repository: PostListRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel(preferencesRepository, repository) {

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    val filteredSubscriptions: LiveData<List<Subscription>> = combine(
        subscriptions,
        _searchQuery
    ) { subscriptions, searchQuery ->
        subscriptions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }.flowOn(defaultDispatcher).asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.updateValue(query)
    }
}
