package com.cosmos.unreddit.ui.base

import androidx.lifecycle.ViewModel
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class BaseViewModel(
    preferencesRepository: PreferencesRepository,
    postListRepository: PostListRepository
) : ViewModel() {

    protected val currentProfile: Flow<Profile> = preferencesRepository.getCurrentProfile().map {
        postListRepository.getProfile(it)
    }
}
