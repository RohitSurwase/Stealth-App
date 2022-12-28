package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import com.cosmos.unreddit.data.repository.AssetsRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetsRepository: AssetsRepository,
    private val currentSource: CurrentSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val nightMode: SharedFlow<Int> = preferencesRepository.getNightMode()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()

    val showNsfw: Flow<Boolean> = preferencesRepository.getShowNsfw()

    val showNsfwPreview: Flow<Boolean> = preferencesRepository.getShowNsfwPreview()

    val showSpoilerPreview: Flow<Boolean> = preferencesRepository.getShowSpoilerPreview()

    val redditSource: SharedFlow<Pair<Int, String>> = combine(
        preferencesRepository.getRedditSource(),
        preferencesRepository.getRedditSourceInstance("teddit.net")
    ) { source, instance ->
        Pair(source, instance)
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    var tedditInstances: List<String> = emptyList()
        private set

    init {
        viewModelScope.launch { loadTedditInstances() }
    }

    private suspend fun loadTedditInstances() {
        assetsRepository.getServiceInstances()
            .onSuccess { services ->
                withContext(defaultDispatcher) {
                    services
                        .firstOrNull { it.service == "reddit" }
                        ?.redirect
                        ?.firstOrNull { it.name == "teddit" }
                        ?.instances
                        ?.let { tedditInstances = it }
                }
            }
    }

    fun setNightMode(nightMode: Int) {
        viewModelScope.launch {
            preferencesRepository.setNightMode(nightMode)
        }
    }

    fun setLeftHandedMode(leftHandedMode: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLeftHandedMode(leftHandedMode)
        }
    }

    fun setShowNsfw(showNsfw: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowNsfw(showNsfw)
        }
    }

    fun setShowNsfwPreview(showNsfwPreview: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowNsfwPreview(showNsfwPreview)
        }
    }

    fun setShowSpoilerPreview(showSpoilerPreview: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSpoilerPreview(showSpoilerPreview)
        }
    }

    fun setRedditSource(source: Int, instance: String?) {
        viewModelScope.launch {
            preferencesRepository.setRedditSource(source)
            currentSource.setRedditSource(source)
            instance?.let { preferencesRepository.setRedditSourceInstance(it) }
        }
    }
}
