package com.cosmos.unreddit.util

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

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
