package com.cosmos.unreddit.ui.postdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.local.mapper.CommentMapper
import com.cosmos.unreddit.data.local.mapper.PostMapper
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PostDetailsViewModel
@Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val repository: PostListRepository
) : BaseViewModel(preferencesRepository, repository) {

    private val _coroutineContext = viewModelScope.coroutineContext + Dispatchers.IO

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _permalink: MutableStateFlow<String?> = MutableStateFlow(null)
    val permalink: StateFlow<String?> = _permalink

    private val _singleThread: MutableLiveData<Boolean> = MutableLiveData(false)
    val singleThread: LiveData<Boolean> = _singleThread

    private val savedCommentIds: Flow<List<String>> = currentProfile.flatMapConcat {
        repository.getSavedCommentIds(it.id).distinctUntilChanged()
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val _listings: MutableStateFlow<Resource<List<Listing>>> =
        MutableStateFlow(Resource.Loading())

    private val _post: Flow<Resource<PostEntity>> = _listings.map {
        when (it) {
            is Resource.Success -> {
                val data = PostMapper.dataToEntity(PostUtil.getPostData(it.data))
                Resource.Success(data)
            }
            is Resource.Loading -> Resource.Loading()
            is Resource.Error -> Resource.Error(it.code, it.message)
        }
    }

    val post: Flow<Resource<PostEntity>> = combine(_post, savedPostIds) { post, savedIds ->
        post.apply {
            if (this is Resource.Success) {
                data.saved = savedIds.contains(data.id)
            }
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val _comments: Flow<Resource<List<Comment>>> = _listings.map {
        when (it) {
            is Resource.Success -> {
                val list = CommentMapper.dataToEntities(
                    PostUtil.getCommentsData(it.data),
                    PostUtil.getPostData(it.data)
                )
                val data = getComments(list, DEPTH_LIMIT)
                Resource.Success(data)
            }
            is Resource.Loading -> Resource.Loading()
            is Resource.Error -> Resource.Error(it.code, it.message)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val comments: Flow<Resource<List<Comment>>> = combine(_comments, savedCommentIds) { comments, savedIds ->
        comments.apply {
            if (this is Resource.Success) {
                data.map { (it as? CommentEntity)?.saved = savedIds.contains(it.name) }
            }
        }
    }

    private suspend fun getComments(list: List<Comment>, depthLimit: Int): List<Comment> {
        return withContext(Dispatchers.Default) {
            val comments = mutableListOf<Comment>()
            for (comment in list) {
                comments.add(comment)
                if (comment is CommentEntity && comment.depth < depthLimit) {
                    comment.isExpanded = true
                    comments.addAll(getComments(comment.replies, depthLimit))
                }
            }
            comments
        }
    }

    private var currentPermalink: String? = null
    private var currentSorting: Sorting? = null

    fun loadPost(forceUpdate: Boolean) {
        if (_permalink.value != null) {
            if (_listings.value == null ||
                _permalink.value != currentPermalink ||
                _sorting.value != currentSorting ||
                forceUpdate
            ) {
                currentPermalink = _permalink.value
                currentSorting = _sorting.value
                loadPost(_permalink.value!!, _sorting.value)
            }
        } else {
            _listings.value = Resource.Error()
        }
    }

    private fun loadPost(permalink: String, sorting: Sorting) {
        viewModelScope.launch {
            repository.getPost(permalink, sorting).onStart {
                _listings.value = Resource.Loading()
            }.catch {
                when (it) {
                    is IOException -> _listings.value = Resource.Error(message = it.message)
                    is HttpException -> _listings.value = Resource.Error(it.code(), it.message())
                    else -> _listings.value = Resource.Error()
                }
            }.collect {
                _listings.value = Resource.Success(it)
            }
        }
    }

    fun insertPostInHistory(postId: String) {
        viewModelScope.launch(_coroutineContext) {
            currentProfile.first().let { repository.insertPostInHistory(postId, it.id) }
        }
    }

    fun setSorting(sorting: Sorting) {
        if (_sorting.value != sorting) {
            _sorting.value = sorting
        }
    }

    fun setPermalink(permalink: String) {
        if (_permalink.value != permalink) {
            _permalink.value = permalink
        }
    }

    fun setSingleThread(singleThread: Boolean) {
        _singleThread.updateValue(singleThread)
    }

    companion object {
        private const val DEPTH_LIMIT = 3
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.BEST)
    }
}
