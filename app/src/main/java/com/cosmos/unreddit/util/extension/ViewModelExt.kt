package com.cosmos.unreddit.util.extension

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

fun <T> MutableLiveData<T>.updateValue(value: T) {
    if (this.value != value) {
        this.value = value
    }
}

fun <T> MutableStateFlow<T>.updateValue(value: T) {
    if (this.value != value) {
        this.value = value
    }
}

val <T> SharedFlow<T>.latest: T? get() = replayCache.lastOrNull()
