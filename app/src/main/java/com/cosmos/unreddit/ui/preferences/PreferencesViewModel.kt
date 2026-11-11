package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import com.cosmos.unreddit.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val currentSource: CurrentSource
) : ViewModel() {

    val nightMode: SharedFlow<Int> = preferencesRepository.getNightMode()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()

    val showNsfw: Flow<Boolean> = preferencesRepository.getShowNsfw()

    val showNsfwPreview: Flow<Boolean> = preferencesRepository.getShowNsfwPreview()

    val showSpoilerPreview: Flow<Boolean> = preferencesRepository.getShowSpoilerPreview()

    val redditSource: SharedFlow<Pair<Int, String>> = combine(
        preferencesRepository.getRedditSource(),
        preferencesRepository.getRedditSourceInstance(REDLIB_INSTANCES.first())
    ) { source, instance ->
        Pair(source, instance)
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    var redlibInstances: List<String> = REDLIB_INSTANCES
        private set

    private companion object {
        // Hardcoded non-onion Redlib instances. Kept static until a
        // dynamic/verified instance list is warranted.
        private val REDLIB_INSTANCES = listOf(
            "redlib.freedit.eu",
            "redlib.cow.rip",
            "redlib.privadency.com"
        )
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
