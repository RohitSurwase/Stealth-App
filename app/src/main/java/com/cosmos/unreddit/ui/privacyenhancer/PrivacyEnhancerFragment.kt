package com.cosmos.unreddit.ui.privacyenhancer

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.ServiceExternal
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.data.model.preferences.DataPreferences
import com.cosmos.unreddit.databinding.LayoutPrivacyEnhancerBinding
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.di.DispatchersModule.MainDispatcher
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.latest
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.parcelable
import com.cosmos.unreddit.util.extension.titlecase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PrivacyEnhancerFragment : PreferenceFragmentCompat() {

    private var _binding: LayoutPrivacyEnhancerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrivacyEnhancerViewModel by viewModels()

    private var privacyEnhancerPreference: SwitchPreferenceCompat? = null

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().theme.applyStyle(R.style.PrivacyPreferenceTheme, true)
        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_privacy_enhancer, rootKey)
        initPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applyWindowInsets(bottom = false)

        _binding = LayoutPrivacyEnhancerBinding.bind(view)

        val list = binding.listContainer.children.find { it is RecyclerView } as RecyclerView?
        list?.apply {
            applyWindowInsets(left = false, top = false, right = false)
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }

        initResultListener()
        bindServices()
    }

    private fun initPreferences() {
        privacyEnhancerPreference = findPreference<SwitchPreferenceCompat>(
            DataPreferences.PreferencesKeys.PRIVACY_ENHANCER.name
        )?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setPrivacyEnhancerEnabled(newValue as Boolean)
                true
            }
        }
    }

    private fun initResultListener() {
        childFragmentManager.setFragmentResultListener(
            PrivacyEnhancerServiceDialog.REQUEST_KEY_REDIRECT,
            viewLifecycleOwner
        ) { _, bundle ->
            val redirect = bundle.parcelable<Redirect>(PrivacyEnhancerServiceDialog.KEY_REDIRECT)
            redirect?.let { viewModel.updateRedirect(it) }
        }
    }

    private fun bindServices() {
        lifecycleScope.launch {
            launch {
                viewModel.instances.collect {
                    when (it) {
                        is Resource.Success -> showLoading(false)
                        is Resource.Loading -> showLoading(true)
                        is Resource.Error -> showLoading(false) // TODO
                    }
                }
            }

            launch {
                viewModel.services.collect { services ->
                    addServicePreferences(services)
                    bindViewModel()
                }
            }
        }
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewModel.redirects.collect { redirects ->
                    viewModel.updateLinkRedirector(redirects)
                    mapRedirectPreferences(redirects)
                }
            }

            launch {
                viewModel.privacyEnhancerEnabled.collect { enabled ->
                    privacyEnhancerPreference?.isChecked = enabled
                }
            }

            launch {
                viewModel.loading.collect { loading ->
                    showLoading(loading)
                }
            }
        }
    }

    private suspend fun mapRedirectPreferences(redirects: List<Redirect>) {
        viewModel.setLoading(true)
        withContext(defaultDispatcher) {
            redirects.forEach {
                val summary = getServiceSummary(it.mode, it.redirect)
                val preference = findPreference<Preference>(it.service)
                withContext(mainDispatcher) {
                    preference?.summary = summary
                }
            }
        }
        viewModel.setLoading(false)
    }

    private fun getServiceSummary(mode: Redirect.RedirectMode, instance: String? = null): String {
        val modeLabel = getString(mode.label)
        return when {
            mode == Redirect.RedirectMode.OFF -> modeLabel
            instance != null -> {
                getString(R.string.preference_privacy_enhancer_service_summary, modeLabel, instance)
            }
            else -> modeLabel
        }
    }

    private fun addServicePreferences(services: List<ServiceExternal>) {
        val servicesCategory = findPreference<PreferenceCategory>("services")
        servicesCategory?.let {
            for (service in services) {
                val servicePreference = Preference(requireContext()).apply {
                    key = service.service
                    title = service.name ?: service.service.titlecase
                    isPersistent = false
                    summary = getString(R.string.redirect_mode_off)
                    setOnPreferenceClickListener {
                        openServicePreferences(service)
                        true
                    }
                }
                it.addPreference(servicePreference)
            }
        }
    }

    private fun openServicePreferences(service: ServiceExternal) {
        val savedRedirect = viewModel.redirects.latest?.find { it.service == service.service }
        PrivacyEnhancerServiceDialog.show(childFragmentManager, service, savedRedirect)
    }

    private fun showLoading(loading: Boolean) {
        binding.run {
            scrim.isVisible = loading
            loadingCradle.isVisible = loading
        }
    }

    override fun onStop() {
        super.onStop()
        childFragmentManager
            .clearFragmentResultListener(PrivacyEnhancerServiceDialog.REQUEST_KEY_REDIRECT)
        // Force theme to be reset to PreferenceTheme to prevent crashes
        requireContext().theme.applyStyle(R.style.PreferenceTheme, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
