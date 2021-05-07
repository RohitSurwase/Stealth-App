package com.cosmos.unreddit.ui.profilemanager

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.ProfileItem
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.databinding.DialogAddProfileBinding
import com.cosmos.unreddit.databinding.FragmentProfileManagerBinding
import com.cosmos.unreddit.ui.common.CarouselPageTransformer
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.text
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileManagerDialogFragment : DialogFragment(), ProfileManagerAdapter.ProfileClickListener {

    private var _binding: FragmentProfileManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileManagerViewModel by viewModels()

    private lateinit var profileAdapter: ProfileManagerAdapter

    private var currentProfile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentProfile = arguments?.getParcelable(KEY_CURRENT_PROFILE) as? Profile
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewPager()
        bindViewModel()
    }

    private fun initViewPager() {
        profileAdapter = ProfileManagerAdapter(currentProfile, this)

        binding.viewPager.apply {
            adapter = profileAdapter
            offscreenPageLimit = 3
            setPageTransformer(CarouselPageTransformer())
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun bindViewModel() {
        viewModel.profiles.asLiveData().observe(viewLifecycleOwner) {
            profileAdapter.submitList(it)
            it.indexOfFirst { item ->
                (item as? ProfileItem.UserProfile)?.profile?.id == currentProfile?.id
            }.let { index ->
                binding.viewPager.currentItem = index
            }
        }
    }

    private fun showAddProfileDialog() {
        val profileBinding = DialogAddProfileBinding.inflate(LayoutInflater.from(requireContext()))
        MaterialAlertDialogBuilder(requireContext())
            .setView(profileBinding.root)
            .setTitle(R.string.dialog_create_profile_title)
            .setPositiveButton(R.string.dialog_create_profile_button) { _, _ ->
                // Ignore
            }
            .setNeutralButton(R.string.dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val name = profileBinding.inputName.text()
                    val errorMessage = validateProfile(name)
                    if (errorMessage == null) {
                        name?.let { viewModel.addProfile(it) }
                        this.dismiss()
                    } else {
                        profileBinding.inputName.error = errorMessage
                    }
                }
            }
    }

    private fun validateProfile(text: String?): String? {
        return if (text?.length !in PROFILE_NAME_MIN..PROFILE_NAME_MAX) {
            getString(R.string.profile_name_length_error)
        } else {
            null
        }
    }

    private fun showDeleteProfileDialog(profile: Profile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_profile_title)
            .setMessage(R.string.dialog_delete_profile_message)
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                viewModel.deleteProfile(profile)
            }
            .setNegativeButton(R.string.dialog_no) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun getTheme(): Int {
        return R.style.ProfileManagerDialogStyle
    }

    override fun onProfileClick(profile: Profile) {
        doAndDismiss { viewModel.selectProfile(profile) }
    }

    override fun onDeleteProfileClick(profile: Profile) {
        showDeleteProfileDialog(profile)
    }

    override fun onNewProfileClick() {
        showAddProfileDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileManagerDialogFragment"

        private const val PROFILE_NAME_MIN = 3
        private const val PROFILE_NAME_MAX = 20

        private const val KEY_CURRENT_PROFILE = "KEY_PROFILE"

        fun show(fragmentManager: FragmentManager, currentProfile: Profile) {
            ProfileManagerDialogFragment().apply {
                arguments = bundleOf(
                    KEY_CURRENT_PROFILE to currentProfile
                )
            }.show(fragmentManager, TAG)
        }
    }
}
