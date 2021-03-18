package com.cosmos.unreddit.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.cosmos.unreddit.R
import com.cosmos.unreddit.preferences.ContentPreferences.PreferencesKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencesFragment : PreferenceFragmentCompat() {

    private val viewModel: PreferencesViewModel by activityViewModels()

    private var nightModePreference: Preference? = null
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
        nightModePreference = findPreference<Preference>(
            UiPreferences.PreferencesKeys.NIGHT_MODE.name
        )?.apply {
            setOnPreferenceClickListener {
                viewModel.nightMode.value?.let { mode ->
                    UiPreferences.NightMode.asIndex(mode)?.let { index ->
                        showNightModeDialog(index)
                    }
                }
                true
            }
        }

        showNsfwPreference = findPreference<SwitchPreferenceCompat>(
            PreferencesKeys.SHOW_NSFW.name
        )?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowNsfw(newValue as Boolean)
                true
            }
        }

        showNsfwPreviewPreference = findPreference<SwitchPreferenceCompat>(
            PreferencesKeys.SHOW_NSFW_PREVIEW.name
        )?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowNsfwPreview(newValue as Boolean)
                true
            }
        }

        showSpoilerPreviewPreference = findPreference<SwitchPreferenceCompat>(
            PreferencesKeys.SHOW_SPOILER_PREVIEW.name
        )?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setShowSpoilerPreview(newValue as Boolean)
                true
            }
        }
    }

    private fun bindViewModel() {
        viewModel.nightMode.observe(viewLifecycleOwner) {
            UiPreferences.NightMode.asIndex(it)?.let { index ->
                val nightModeArray = resources.getStringArray(R.array.pref_night_mode_labels)
                nightModePreference?.summary = nightModeArray.getOrNull(index)
            }
        }
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

    private fun showNightModeDialog(checkedItem: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_night_mode_title)
            .setSingleChoiceItems(R.array.pref_night_mode_labels, checkedItem) { dialog, which ->
                UiPreferences.NightMode.asMode(which)?.let { mode ->
                    updateNightMode(mode)
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun updateNightMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        viewModel.setNightMode(mode)
    }

    companion object {
        const val TAG = "PreferencesFragment"
    }
}
