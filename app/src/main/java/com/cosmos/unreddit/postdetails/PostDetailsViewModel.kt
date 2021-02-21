package com.cosmos.unreddit.postdetails

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.api.pojo.details.Listing
import com.cosmos.unreddit.api.pojo.details.PostChild
import com.cosmos.unreddit.database.CommentMapper
import com.cosmos.unreddit.database.PostMapper
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.withContext

class PostDetailsViewModel
@ViewModelInject constructor(private val repository: PostListRepository) : ViewModel() {

    private val _cachedPost: MediatorLiveData<PostEntity> = MediatorLiveData()
    val cachedPost: LiveData<PostEntity>
        get() = _cachedPost

    private val _sorting: MutableStateFlow<Sorting> = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _permalink: MutableStateFlow<String?> = MutableStateFlow(null)
    val permalink: StateFlow<String?> = _permalink

    private val _listings: LiveData<List<Listing>> = combineTransform(
        _permalink,
        _sorting
    ) { _permalink, _sorting ->
        _permalink?.let { permalink ->
            repository.getPost(permalink, _sorting).collect { emit(it) }
        }
    }.asLiveData()

    val post: LiveData<PostEntity> = _listings.switchMap {
        liveData { emit(PostMapper.dataToEntity((it[0].data.children[0] as PostChild).data)) }
    }

    val comments: LiveData<List<Comment>> = _listings.switchMap {
        liveData {
            emit(getComments(CommentMapper.dataToEntities(it[1].data.children), DEPTH_LIMIT))
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

    fun setPost(post: PostEntity) {
        if (_cachedPost.value != post) {
            _cachedPost.value = post
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

    companion object {
        private const val DEPTH_LIMIT = 3
        private val DEFAULT_SORTING = Sorting(RedditApi.Sort.BEST)
    }
}
