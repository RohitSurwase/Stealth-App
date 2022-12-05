package com.cosmos.unreddit.ui.privacyenhancer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.ServiceExternal
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.data.repository.AssetsRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.util.LinkRedirector
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyEnhancerViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetsRepository: AssetsRepository,
    private val linkRedirector: LinkRedirector,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val privacyEnhancerEnabled: Flow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()

    val redirects: SharedFlow<List<Redirect>> = preferencesRepository
        .getAllRedirects()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean>
        get() = _loading.asStateFlow()

    private val _instances: MutableStateFlow<Resource<List<ServiceExternal>>> =
        MutableStateFlow(Resource.Loading())
    val instances: StateFlow<Resource<List<ServiceExternal>>>
        get() = _instances.asStateFlow()

    val services: Flow<List<String>> = _instances
        .filter { instance -> instance is Resource.Success }
        .map { instance -> (instance as Resource.Success).data.map { it.service } }
        .map { it.sorted() }
        .flowOn(defaultDispatcher)

    init {
        loadServiceInstances()
    }

    fun setPrivacyEnhancerEnabled(enable: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPrivacyEnhancerEnabled(enable)
            linkRedirector.setPrivacyEnhancerEnabled(enable)
        }
    }

    fun updateLinkRedirector(redirects: List<Redirect>) {
        viewModelScope.launch {
            linkRedirector.setRedirectList(redirects)
        }
    }

    fun setLoading(loading: Boolean) {
        _loading.updateValue(loading)
    }

    fun updateRedirect(redirect: Redirect) {
        viewModelScope.launch {
            preferencesRepository.updateRedirect(redirect)
        }
    }

    private fun loadServiceInstances() {
        viewModelScope.launch {
            assetsRepository.getServiceInstances()
                .onSuccess { _instances.value = Resource.Success(it) }
                .onFailure { _instances.value = Resource.Error(throwable = it) }
        }
    }
}
