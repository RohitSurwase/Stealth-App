package com.cosmos.unreddit.data.repository

import com.cosmos.unreddit.data.remote.api.gfycat.GfycatApi
import com.cosmos.unreddit.data.remote.api.gfycat.model.Item
import com.cosmos.unreddit.di.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GfycatRepository @Inject constructor(
    @NetworkModule.Gfycat private val gfycatApi: GfycatApi,
    @NetworkModule.Redgifs private val redgifsApi: GfycatApi
) {

    fun getGfycatGif(id: String): Flow<Item> = flow {
        emit(gfycatApi.getGif(id))
    }

    fun getRedgifsGif(id: String): Flow<Item> = flow {
        emit(redgifsApi.getGif(id))
    }

    companion object {
        private const val TIMEOUT = 10000
    }
}
