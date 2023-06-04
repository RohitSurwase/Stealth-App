package com.cosmos.unreddit.ui.policydisclaimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PolicyDisclaimerViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    fun setPolicyDisclaimerShown(shown: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPolicyDisclaimerShown(shown)
        }
    }
}
