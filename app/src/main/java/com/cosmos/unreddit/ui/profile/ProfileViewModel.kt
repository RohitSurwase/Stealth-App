package com.cosmos.unreddit.ui.profile

import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.local.mapper.SavedMapper
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.SavedItem
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    repository: PostListRepository
) : BaseViewModel(preferencesRepository, repository) {

    val contentPreferences: Flow<ContentPreferences> = preferencesRepository.getContentPreferences()

    private val _page: MutableStateFlow<Int> = MutableStateFlow(0)
    val page: StateFlow<Int> get() = _page

    private val _savedPosts: Flow<List<PostEntity>> = currentProfile.flatMapMerge {
        repository.getSavedPosts(it.id)
    }

    private val _savedComments: Flow<List<Comment.CommentEntity>> = currentProfile.flatMapMerge {
        repository.getSavedComments(it.id)
    }

    val selectedProfile: Flow<Profile> = combine(
        currentProfile,
        repository.getAllProfiles()
    ) { currentProfile, profiles ->
        // Update current profile when any profile is updated
        profiles.find { it.id == currentProfile.id } ?: currentProfile
    }

    val savedItems: Flow<List<SavedItem>> = combineTransform(
        _savedPosts,
        _savedComments,
        contentPreferences
    ) { _posts, _comments, preferences ->
        val posts = viewModelScope.async {
            SavedMapper.postsToEntities(_posts).filter {
                preferences.showNsfw || !(it as SavedItem.Post).post.isOver18
            }
        }

        val comments = viewModelScope.async {
            SavedMapper.commentsToEntities(_comments)
        }

        val items = mutableListOf<SavedItem>().apply {
            addAll(posts.await())
            addAll(comments.await())
        }

        emit(items)
    }.map { items ->
        items.sortedByDescending { it.timestamp }
    }

    fun setPage(position: Int) {
        _page.updateValue(position)
    }
}
