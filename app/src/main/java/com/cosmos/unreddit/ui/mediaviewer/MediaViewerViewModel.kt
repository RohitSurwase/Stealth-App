package com.cosmos.unreddit.ui.mediaviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.local.mapper.PostMapper
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.GalleryMedia.Type
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.repository.GfycatRepository
import com.cosmos.unreddit.data.repository.ImgurRepository
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.StreamableRepository
import com.cosmos.unreddit.util.LinkUtil
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel
@Inject constructor(
    private val imgurRepository: ImgurRepository,
    private val streamableRepository: StreamableRepository,
    private val gfycatRepository: GfycatRepository,
    private val postListRepository: PostListRepository
) : ViewModel() {

    private val _media: MutableLiveData<Resource<List<GalleryMedia>>> = MutableLiveData()
    val media: LiveData<Resource<List<GalleryMedia>>> get() = _media

    private val _selectedPage: MutableLiveData<Int> = MutableLiveData()
    val selectedPage: LiveData<Int> get() = _selectedPage

    fun loadMedia(link: String, mediaType: MediaType, forceUpdate: Boolean = false) {
        if (_media.value == null || forceUpdate) {
            retrieveMedia(link, mediaType)
        }
    }

    private fun retrieveMedia(link: String, mediaType: MediaType) {
        viewModelScope.launch {
            when (mediaType) {
                MediaType.IMGUR_IMAGE, MediaType.IMGUR_LINK, MediaType.IMAGE -> {
                    setMedia(GalleryMedia.singleton(Type.IMAGE, link))
                }
                MediaType.IMGUR_GIF -> {
                    setMedia(GalleryMedia.singleton(Type.VIDEO, LinkUtil.getImgurVideo(link)))
                }
                MediaType.REDDIT_GIF, MediaType.IMGUR_VIDEO, MediaType.VIDEO -> {
                    setMedia(GalleryMedia.singleton(Type.VIDEO, link))
                }
                MediaType.REDDIT_VIDEO -> {
                    setMedia(
                        GalleryMedia.singleton(Type.VIDEO, link, LinkUtil.getRedditSoundTrack(link))
                    )
                }
                MediaType.GFYCAT, MediaType.REDGIFS -> {
                    val id = LinkUtil.getGfycatId(link)

                    val gifItem = if (mediaType == MediaType.GFYCAT) {
                        gfycatRepository.getGfycatGif(id)
                    } else {
                        gfycatRepository.getRedgifsGif(id)
                    }

                    gifItem.onStart {
                        _media.value = Resource.Loading()
                    }.catch {
                        catchError(it)
                    }.map {
                        GalleryMedia.singleton(Type.VIDEO, it.gfyItem.contentUrls.mp4.url)
                    }.collect {
                        setMedia(it)
                    }
                }
                MediaType.STREAMABLE -> {
                    val shortcode = LinkUtil.getStreamableShortcode(link)
                    streamableRepository.getVideo(shortcode).onStart {
                        _media.value = Resource.Loading()
                    }.catch {
                        catchError(it)
                    }.map { video ->
                        GalleryMedia.singleton(Type.VIDEO, video.files.mp4.url)
                    }.collect {
                        setMedia(it)
                    }
                }
                MediaType.IMGUR_ALBUM, MediaType.IMGUR_GALLERY -> {
                    val albumId = LinkUtil.getAlbumIdFromImgurLink(link)
                    imgurRepository.getAlbum(albumId).onStart {
                        _media.value = Resource.Loading()
                    }.catch {
                        catchError(it)
                    }.map { album ->
                        album.data.images.map { image ->
                            GalleryMedia(
                                if (image.preferVideo) Type.VIDEO else Type.IMAGE,
                                LinkUtil.getUrlFromImgurImage(image),
                                description = image.description
                            )
                        }
                    }.collect {
                        setMedia(it)
                    }
                }
                MediaType.REDDIT_GALLERY -> {
                    val permalink = LinkUtil.getPermalinkFromMediaUrl(link)
                    postListRepository.getPost(permalink, Sorting(RedditApi.Sort.BEST)).onStart {
                        _media.value = Resource.Loading()
                    }.catch {
                        catchError(it)
                    }.map { listings ->
                        PostMapper.dataToEntity(PostUtil.getPostData(listings)).gallery
                    }.collect {
                        setMedia(it)
                    }
                }
                else -> {
                    _media.value = Resource.Error()
                }
            }
        }
    }

    private fun catchError(throwable: Throwable) {
        when (throwable) {
            is IOException -> _media.value = Resource.Error(message = throwable.message)
            is HttpException -> _media.value = Resource.Error(throwable.code(), throwable.message())
            else -> _media.value = Resource.Error()
        }
    }

    fun setMedia(media: List<GalleryMedia>) {
        _media.updateValue(Resource.Success(media))
    }

    fun setSelectedPage(position: Int) {
        _selectedPage.updateValue(position)
    }
}
