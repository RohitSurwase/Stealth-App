package com.cosmos.unreddit.data.model

sealed class Resource<out T> {

    val dataValue: T?
        get() = when (this) {
            is Success -> data
            is Loading -> null
            is Error -> null
        }

    data class Success<out T>(val data: T) : Resource<T>()

    class Loading<out T> : Resource<T>()

    data class Error<out T>(
        val code: Int? = null,
        val message: String? = null
    ) : Resource<T>()
}
