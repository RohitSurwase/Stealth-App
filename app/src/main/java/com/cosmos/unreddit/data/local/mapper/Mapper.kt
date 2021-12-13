package com.cosmos.unreddit.data.local.mapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class Mapper<From, To>(protected val defaultDispatcher: CoroutineDispatcher) {

    protected abstract suspend fun toEntity(from: From): To

    protected open suspend fun toEntities(from: List<From>): List<To> {
        return from.map { toEntity(it) }
    }

    suspend fun dataToEntity(from: From): To = withContext(defaultDispatcher) {
        toEntity(from)
    }

    suspend fun dataToEntities(from: List<From>): List<To> = withContext(defaultDispatcher) {
        toEntities(from)
    }
}
