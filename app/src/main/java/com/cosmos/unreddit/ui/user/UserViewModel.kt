package com.cosmos.unreddit.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.cosmos.unreddit.data.local.mapper.CommentMapper2
import com.cosmos.unreddit.data.local.mapper.PostMapper2
import com.cosmos.unreddit.data.local.mapper.UserMapper
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Data
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: PostListRepository,
    preferencesRepository: PreferencesRepository,
    private val postMapper: PostMapper2,
    private val commentMapper: CommentMapper2,
    @DispatchersModule.DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel(preferencesRepository, repository) {

    val contentPreferences: Flow<ContentPreferences> =
        preferencesRepository.getContentPreferences()

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _user: MutableStateFlow<String> = MutableStateFlow("")
    val user: StateFlow<String> = _user

    private val _page: MutableStateFlow<Int> = MutableStateFlow(0)
    val page: StateFlow<Int> get() = _page

    private val _about: MutableLiveData<Resource<User>> = MutableLiveData()
    val about: LiveData<Resource<User>> = _about

    private val savedCommentIds: Flow<List<String>> = currentProfile.flatMapLatest {
        repository.getSavedCommentIds(it.id).distinctUntilChanged()
    }

    val postDataFlow: Flow<PagingData<PostEntity>>
    val commentDataFlow: Flow<PagingData<Comment>>

    private val searchData: StateFlow<Data.Fetch> = combine(
        user,
        sorting
    ) { user, sorting ->
        Data.Fetch(user, sorting)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Data.Fetch("", DEFAULT_SORTING)
    )

    private val userData: Flow<Data.User> = combine(
        historyIds,
        savedPostIds,
        contentPreferences,
        savedCommentIds
    ) { history, saved, prefs, savedComments ->
        Data.User(history, saved, prefs, savedComments)
    }

    val data: Flow<Pair<Data.Fetch, Data.User>> = searchData
        .dropWhile { it.query.isBlank() }
        .flatMapLatest { searchData -> userData.take(1).map { searchData to it } }

    init {
        postDataFlow = data
            .flatMapLatest { data -> getPosts(data.first, data.second) }
            .cachedIn(viewModelScope)

        commentDataFlow = data
            .flatMapLatest { data -> getComments(data.first, data.second) }
            .cachedIn(viewModelScope)
    }

    private fun getPosts(
        data: Data.Fetch,
        user: Data.User
    ): Flow<PagingData<PostEntity>> {
        return repository.getUserPosts(data.query, data.sorting)
            .map { pagingData ->
                PostUtil.filterPosts(pagingData, user, postMapper, defaultDispatcher)
            }
    }

    private fun getComments(
        data: Data.Fetch,
        user: Data.User
    ): Flow<PagingData<Comment>> {
        return repository.getUserComments(data.query, data.sorting)
            .map { pagingData ->
                pagingData
                    .map { commentMapper.dataToEntity(it, null) }
                    .map { comment ->
                        comment.apply {
                            (this as? Comment.CommentEntity)?.saved =
                                user.savedComments?.contains(this.name) ?: false
                        }
                    }
            }
    }

    fun loadUserInfo(forceUpdate: Boolean) {
        if (_user.value.isNotBlank()) {
            if (_about.value == null || forceUpdate) {
                loadUserInfo(_user.value)
            }
        } else {
            _about.value = Resource.Error()
        }
    }

    private fun loadUserInfo(user: String) {
        viewModelScope.launch {
            repository.getUserInfo(user).onStart {
                _about.value = Resource.Loading()
            }.catch {
                when (it) {
                    is IOException -> _about.value = Resource.Error(message = it.message)
                    is HttpException -> _about.value = Resource.Error(it.code(), it.message())
                    else -> _about.value = Resource.Error()
                }
            }.map {
                UserMapper.dataToEntity(it.data)
            }.collect {
                _about.value = Resource.Success(it)
            }
        }
    }

    fun setSorting(sorting: Sorting) {
        _sorting.updateValue(sorting)
    }

    fun setUser(user: String) {
        _user.updateValue(user)
    }

    fun setPage(position: Int) {
        _page.updateValue(position)
    }

    companion object {
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.NEW)
    }
}
