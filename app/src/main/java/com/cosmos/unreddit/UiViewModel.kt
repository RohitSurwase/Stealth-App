package com.cosmos.unreddit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UiViewModel : ViewModel() {

    private val _navigationVisibility = MutableLiveData<Boolean>()
    val navigationVisibility: LiveData<Boolean> get() = _navigationVisibility

    fun setNavigationVisibility(visible: Boolean) {
        if (_navigationVisibility.value != visible) {
            _navigationVisibility.value = visible
        }
    }
}
