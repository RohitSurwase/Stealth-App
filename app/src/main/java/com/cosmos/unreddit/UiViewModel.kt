package com.cosmos.unreddit

import androidx.lifecycle.ViewModel
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UiViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()

    private val _navigationVisibility = MutableStateFlow(true)
    val navigationVisibility: StateFlow<Boolean> = _navigationVisibility

    fun setNavigationVisibility(visible: Boolean) {
        _navigationVisibility.updateValue(visible)
    }
}
