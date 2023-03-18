package com.cosmos.unreddit.data.repository

import com.cosmos.unreddit.data.remote.api.gfycat.GfycatApi
import com.cosmos.unreddit.data.remote.api.gfycat.model.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GfycatRepository @Inject constructor(
    private val gfycatApi: GfycatApi
) {

    fun getGfycatGif(id: String): Flow<Item> = flow {
        emit(gfycatApi.getGif(id))
    }
}
