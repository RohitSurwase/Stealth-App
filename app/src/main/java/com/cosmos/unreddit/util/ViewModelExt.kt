package com.cosmos.unreddit.util

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.updateValue(value: T) {
    if (this.value != value) {
        this.value = value
    }
}
