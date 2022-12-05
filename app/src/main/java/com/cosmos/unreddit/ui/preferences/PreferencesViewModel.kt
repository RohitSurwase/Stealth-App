package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import com.cosmos.unreddit.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
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

    val redditSource: SharedFlow<Int> = preferencesRepository.getRedditSource()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

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

    fun setRedditSource(source: Int) {
        viewModelScope.launch {
            preferencesRepository.setRedditSource(source)
            currentSource.setRedditSource(source)
        }
    }
}
