package com.cosmos.unreddit.ui.postlist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cosmos.stealth.sdk.data.model.api.ServiceName
import com.cosmos.stealth.sdk.data.model.api.Sort
import com.cosmos.unreddit.data.local.mapper.FeedableMapper
import com.cosmos.unreddit.data.model.Data
import com.cosmos.unreddit.data.model.Filtering
import com.cosmos.unreddit.data.model.Service
import com.cosmos.unreddit.data.model.ServiceQuery
import com.cosmos.unreddit.data.model.db.FeedItem
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.repository.DatabaseRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.data.repository.StealthRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.extension.updateValue
import com.cosmos.unreddit.util.mapFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PostListViewModel
@Inject constructor(
    databaseRepository: DatabaseRepository,
    private val stealthRepository: StealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val feedableMapper: FeedableMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel(preferencesRepository, databaseRepository) {

    val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    val profiles: Flow<List<Profile>> = databaseRepository.getAllProfiles()

    private val _filtering: MutableStateFlow<Filtering> = MutableStateFlow(DEFAULT_FILTERING)
    val filtering: StateFlow<Filtering> = _filtering

    val communities: Flow<List<ServiceQuery>> = subscriptions
        .distinctUntilChanged()
        .map { subscriptions ->
            if (subscriptions.isNotEmpty()) {
                getServiceQueries(subscriptions)
            } else {
                DEFAULT_QUERY
            }
        }
        .flowOn(defaultDispatcher)

    val feedItemDataFlow: Flow<PagingData<FeedItem>>

    val fetchData: StateFlow<Data.FetchMultiple> = combine(
        communities,
        filtering
    ) { communities, filtering ->
        Data.FetchMultiple(communities, filtering)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Data.FetchMultiple(DEFAULT_QUERY, DEFAULT_FILTERING)
    )

    private var latestUser: Data.User? = null

    private val userData: Flow<Data.User> = combine(
        historyIds, savedPostIds, contentPreferences
    ) { history, saved, prefs ->
        Data.User(history, saved, prefs)
    }.onEach {
        latestUser = it
    }.distinctUntilChangedBy {
        it.contentPreferences
    }

    private val _lastRefresh: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis())
    val lastRefresh: StateFlow<Long> = _lastRefresh.asStateFlow()

    var isDrawerOpen: Boolean = false

    init {
        feedItemDataFlow = fetchData
            // Fetch last user data when search data is updated and merge them together
            .flatMapLatest { fetchData -> userData.map { fetchData to it } }
            .flatMapLatest { getFeedItems(it.first, it.second) }
            .onEach { _lastRefresh.value = System.currentTimeMillis() }
            .cachedIn(viewModelScope)
    }

    private fun getFeedItems(
        data: Data.FetchMultiple,
        user: Data.User
    ): Flow<PagingData<FeedItem>> {
        return stealthRepository.getFeed(data.query, data.filtering)
            .map { it.mapFilter(latestUser ?: user, feedableMapper, defaultDispatcher) }
    }

    fun setFiltering(filtering: Filtering) {
        _filtering.updateValue(filtering)
    }

    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            preferencesRepository.setCurrentProfile(profile.id)
        }
    }

    private suspend fun getServiceQueries(
        subscriptions: List<Subscription>
    ): List<ServiceQuery> = withContext(defaultDispatcher) {
        val services = mutableMapOf<ServiceName, MutableMap<String, MutableList<String>>>()

        // Group subscriptions by service, then by instance
        subscriptions.forEach { subscription ->
            val service = services[subscription.service] ?: mutableMapOf()
            val queries = service[subscription.instance] ?: mutableListOf()

            queries.add(subscription.name)

            service[subscription.instance] = queries
            services[subscription.service] = service
        }

        val serviceQueries = mutableListOf<ServiceQuery>()

        services.forEach { serviceEntry ->
            val serviceName = serviceEntry.key

            serviceEntry.value.forEach { instanceEntry ->
                val service = Service(serviceName, instanceEntry.key)
                val serviceQuery = ServiceQuery(service, instanceEntry.value.toList())

                serviceQueries.add(serviceQuery)
            }
        }

        serviceQueries.toList()
    }

    companion object {
        private const val DEFAULT_LEMMY_INSTANCE = "lemmy.world"

        // Pass empty lists to fetch default feed of each service
        // Reddit -> popular
        // Lemmy -> front page
        private val DEFAULT_QUERY = buildList {
            add(ServiceQuery(Service(ServiceName.reddit), emptyList()))
            add(ServiceQuery(Service(ServiceName.lemmy, DEFAULT_LEMMY_INSTANCE), emptyList()))
        }

        private val DEFAULT_FILTERING = Filtering(Sort.trending)
    }
}
