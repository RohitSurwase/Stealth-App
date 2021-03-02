package com.cosmos.unreddit.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.cosmos.unreddit.R
import com.cosmos.unreddit.preferences.ContentPreferences.PreferencesKeys
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencesFragment : PreferenceFragmentCompat() {

    private val viewModel: PreferencesViewModel by activityViewModels()

    private var showNsfwPreference: SwitchPreferenceCompat? = null
    private var showNsfwPreviewPreference: SwitchPreferenceCompat? = null
    private var showSpoilerPreviewPreference: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        initPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    private fun initPreferences() {
        showNsfwPreference = findPreference<SwitchPreferenceCompat>(PreferencesKeys.SHOW_NSFW.name)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowNsfw(newValue as Boolean)
                true
            }
        }
        showNsfwPreviewPreference = findPreference<SwitchPreferenceCompat>(PreferencesKeys.SHOW_NSFW_PREVIEW.name)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowNsfwPreview(newValue as Boolean)
                true
            }
        }
        showSpoilerPreviewPreference = findPreference<SwitchPreferenceCompat>(PreferencesKeys.SHOW_SPOILER_PREVIEW.name)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowSpoilerPreview(newValue as Boolean)
                true
            }
        }
    }

    private fun bindViewModel() {
        viewModel.showNsfw.observe(viewLifecycleOwner, { showNsfw ->
            showNsfwPreference?.isChecked = showNsfw
            showNsfwPreviewPreference?.isEnabled = showNsfw
        })
        viewModel.showNsfwPreview.observe(viewLifecycleOwner, { showNsfwPreview ->
            showNsfwPreviewPreference?.isChecked = showNsfwPreview
        })
        viewModel.showSpoilerPreview.observe(viewLifecycleOwner, { showSpoilerPreview ->
            showSpoilerPreviewPreference?.isChecked = showSpoilerPreview
        })
    }

    companion object {
        const val TAG = "PreferencesFragment"
    }
}