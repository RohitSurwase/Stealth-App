package com.cosmos.unreddit.repository

import com.cosmos.unreddit.api.streamable.StreamableApi
import com.cosmos.unreddit.api.streamable.pojo.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamableRepository @Inject constructor(private val streamableApi: StreamableApi) {

    fun getVideo(shortcode: String): Flow<Video> = flow {
        emit(streamableApi.getVideo(shortcode))
    }
}
