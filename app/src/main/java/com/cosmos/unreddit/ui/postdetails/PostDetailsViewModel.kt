package com.cosmos.unreddit.ui.postdetails

import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.local.mapper.CommentMapper2
import com.cosmos.unreddit.data.local.mapper.PostMapper2
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PostDetailsViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val repository: PostListRepository,
    private val postMapper: PostMapper2,
    private val commentMapper: CommentMapper2,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel(preferencesRepository, repository) {

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _permalink: MutableStateFlow<String?> = MutableStateFlow(null)
    val permalink: StateFlow<String?> = _permalink

    private val _singleThread: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val singleThread: StateFlow<Boolean> = _singleThread

    val savedCommentIds: Flow<List<String>> = currentProfile.flatMapLatest {
        repository.getSavedCommentIds(it.id)
    }

    private val _post: MutableStateFlow<Resource<PostEntity>> =
        MutableStateFlow(Resource.Loading())

    private val _comments: MutableStateFlow<Resource<List<Comment>>> =
        MutableStateFlow(Resource.Loading())

    val post: Flow<Resource<PostEntity>> = combine(_post, savedPostIds) { post, savedIds ->
        post.apply {
            if (this is Resource.Success) {
                data.saved = savedIds.contains(data.id)
            }
        }
    }.flowOn(defaultDispatcher)

    val comments: Flow<Resource<List<Comment>>> = combine(
        _comments,
        savedCommentIds
    ) { comments, savedIds ->
        comments.apply {
            if (this is Resource.Success) {
                data.forEach { comment ->
                    (comment as? CommentEntity)?.saved = savedIds.contains(comment.name)
                }
            }
        }
    }.distinctUntilChanged().flowOn(defaultDispatcher)

    private suspend fun getComments(list: List<Comment>, depthLimit: Int): List<Comment> =
        withContext(defaultDispatcher) {
            val comments = mutableListOf<Comment>()
            for (comment in list) {
                comments.add(comment)
                if (comment is CommentEntity && comment.depth < depthLimit) {
                    comment.isExpanded = true
                    comments.addAll(getComments(comment.replies, depthLimit))
                }
            }
            return@withContext comments
        }

    private var currentPermalink: String? = null
    private var currentSorting: Sorting? = null

    fun loadPost(forceUpdate: Boolean) {
        if (_permalink.value != null) {
            if (
                _permalink.value != currentPermalink ||
                _sorting.value != currentSorting ||
                forceUpdate
            ) {
                currentPermalink = _permalink.value
                currentSorting = _sorting.value
                loadPost(_permalink.value!!, _sorting.value)
            }
        } else {
            _post.value = Resource.Error()
            _comments.value = Resource.Error()
        }
    }

    private fun loadPost(permalink: String, sorting: Sorting) {
        viewModelScope.launch {
            repository.getPost(permalink, sorting)
                .onStart {
                    _post.value = Resource.Loading()
                    _comments.value = Resource.Loading()
                }
                .catch { e ->
                    when (e) {
                        is IOException -> {
                            _post.value = Resource.Error(message = e.message)
                            _comments.value = Resource.Error(message = e.message)
                        }
                        is HttpException -> {
                            _post.value = Resource.Error(e.code(), e.message())
                            _comments.value = Resource.Error(e.code(), e.message())
                        }
                        else -> {
                            _post.value = Resource.Error()
                            _comments.value = Resource.Error()
                        }
                    }
                }
                .collect { listings ->
                    val post = async { postMapper.dataToEntity(PostUtil.getPostData(listings)) }
                    val comments = async {
                        val list = commentMapper.dataToEntities(
                            PostUtil.getCommentsData(listings),
                            PostUtil.getPostData(listings)
                        )
                        getComments(list, DEPTH_LIMIT)
                    }
                    _post.value = Resource.Success(post.await())
                    _comments.value = Resource.Success(comments.await())
                }
        }
    }

    fun setSorting(sorting: Sorting) {
        _sorting.updateValue(sorting)
    }

    fun setPermalink(permalink: String) {
        _permalink.updateValue(permalink)
    }

    fun setSingleThread(singleThread: Boolean) {
        _singleThread.updateValue(singleThread)
    }

    fun setComments(comments: List<Comment>) {
        _comments.updateValue(Resource.Success(comments))
    }

    companion object {
        private const val DEPTH_LIMIT = 3
        private val DEFAULT_SORTING = Sorting(Sort.BEST)
    }
}
