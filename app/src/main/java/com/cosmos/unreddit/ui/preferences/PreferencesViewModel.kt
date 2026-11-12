package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.data.repository.RedlibInstancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val currentSource: CurrentSource,
    private val redlibInstancesRepository: RedlibInstancesRepository
) : ViewModel() {

    val nightMode: SharedFlow<Int> = preferencesRepository.getNightMode()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()

    val showNsfw: Flow<Boolean> = preferencesRepository.getShowNsfw()

    val showNsfwPreview: Flow<Boolean> = preferencesRepository.getShowNsfwPreview()

    val showSpoilerPreview: Flow<Boolean> = preferencesRepository.getShowSpoilerPreview()

    val customUserAgent: SharedFlow<String> = preferencesRepository.getCustomUserAgent()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val redditSource: SharedFlow<Pair<Int, String>> = combine(
        preferencesRepository.getRedditSource(),
        preferencesRepository.getRedditSourceInstance(DEFAULT_INSTANCE)
    ) { source, instance ->
        Pair(source, instance)
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    /**
     * Uses the cached instance list and only downloads it when nothing has been
     * cached yet. GitHub is not queried on every dialog/opening.
     */
    var redlibInstances: List<String> = redlibInstancesRepository
        .getCachedInstances()
        .ifEmpty { listOf(DEFAULT_INSTANCE) }
        private set

    init {
        viewModelScope.launch {
            redlibInstancesRepository.getInstances()
                .onSuccess { redlibInstances = it }
        }
    }

    suspend fun refreshRedlibInstances(): Boolean {
        return redlibInstancesRepository.getInstances(forceRefresh = true)
            .onSuccess { redlibInstances = it }
            .isSuccess
    }

    fun setCustomUserAgent(userAgent: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomUserAgent(userAgent)
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

    private companion object {
        /**
         * Only used before the online instance list has been downloaded for the
         * first time, so the app always has a usable instance.
         */
        private val DEFAULT_INSTANCE: String = TedditApi.BASE_URL.toHttpUrl().host
    }
}
