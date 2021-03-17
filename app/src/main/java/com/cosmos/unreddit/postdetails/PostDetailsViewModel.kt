package com.cosmos.unreddit.postdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.api.Resource
import com.cosmos.unreddit.api.pojo.details.Listing
import com.cosmos.unreddit.api.pojo.details.PostChild
import com.cosmos.unreddit.database.CommentMapper
import com.cosmos.unreddit.database.PostMapper
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.util.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PostDetailsViewModel
@Inject constructor(private val repository: PostListRepository) : ViewModel() {

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _permalink: MutableStateFlow<String?> = MutableStateFlow(null)
    val permalink: StateFlow<String?> = _permalink

    private val _singleThread: MutableLiveData<Boolean> = MutableLiveData(false)
    val singleThread: LiveData<Boolean> = _singleThread

    private val _listings: MutableLiveData<Resource<List<Listing>>> = MutableLiveData()

    val post: LiveData<Resource<PostEntity>> = _listings.switchMap {
        liveData {
            val resource = when (it) {
                is Resource.Success -> {
                    val data = PostMapper.dataToEntity(
                        (it.data[0].data.children[0] as PostChild).data
                    )
                    Resource.Success(data)
                }
                is Resource.Loading -> Resource.Loading()
                is Resource.Error -> Resource.Error(it.code, it.message)
            }
            emit(resource)
        }
    }

    val comments: LiveData<Resource<List<Comment>>> = _listings.switchMap {
        liveData {
            val resource = when (it) {
                is Resource.Success -> {
                    val list = CommentMapper.dataToEntities(it.data[1].data.children)
                    val data = getComments(list, DEPTH_LIMIT)
                    Resource.Success(data)
                }
                is Resource.Loading -> Resource.Loading()
                is Resource.Error -> Resource.Error(it.code, it.message)
            }
            emit(resource)
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
