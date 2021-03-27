package com.cosmos.unreddit.data.repository

import com.cosmos.unreddit.data.remote.api.imgur.ImgurApi
import com.cosmos.unreddit.data.remote.api.imgur.model.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImgurRepository @Inject constructor(private val imgurApi: ImgurApi) {

    fun getAlbum(albumId: String): Flow<Album> = flow {
        emit(imgurApi.getAlbum(albumId))
    }
}
