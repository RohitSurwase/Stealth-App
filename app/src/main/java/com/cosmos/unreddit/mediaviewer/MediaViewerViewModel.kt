package com.cosmos.unreddit.mediaviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.model.GalleryMedia
import com.cosmos.unreddit.model.GalleryMedia.Type
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.parser.link.LinkParser
import com.cosmos.unreddit.repository.ImgurRepository
import com.cosmos.unreddit.repository.StreamableRepository
import com.cosmos.unreddit.util.LinkUtil
import com.cosmos.unreddit.util.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel
@Inject constructor(
    private val imgurRepository: ImgurRepository,
    private val streamableRepository: StreamableRepository
) : ViewModel() {

    private val _media: MutableLiveData<List<GalleryMedia>> = MutableLiveData()
    val media: LiveData<List<GalleryMedia>> get() = _media

    private val _selectedPage: MutableLiveData<Int> = MutableLiveData()
    val selectedPage: LiveData<Int> get() = _selectedPage

    fun loadMedia(link: String, mediaType: MediaType) {
        if (_media.value == null) {
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
                MediaType.GFYCAT -> {
                    setMedia(GalleryMedia.singleton(Type.VIDEO, LinkUtil.getGfycatVideo(link)))
                }
                MediaType.REDGIFS -> {
                    setMedia(LinkParser.parseRedgifsLink(link))
                }
                MediaType.STREAMABLE -> {
                    val shortcode = LinkUtil.getStreamableShortcode(link)
                    streamableRepository.getVideo(shortcode).map { video ->
                        GalleryMedia.singleton(Type.VIDEO, video.files.mp4.url)
                    }.collect {
                        setMedia(it)
                    }
                }
                MediaType.IMGUR_ALBUM, MediaType.IMGUR_GALLERY -> {
                    val albumId = LinkUtil.getAlbumIdFromImgurLink(link)
                    imgurRepository.getAlbum(albumId).map { album ->
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
                else -> {}
            }
        }
    }

    fun setMedia(media: List<GalleryMedia>) {
        _media.updateValue(media)
    }

    fun setSelectedPage(position: Int) {
        _selectedPage.updateValue(position)
    }
}
