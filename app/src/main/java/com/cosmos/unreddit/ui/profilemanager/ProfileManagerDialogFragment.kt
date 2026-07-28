package com.cosmos.unreddit.ui.profilemanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.ProfileItem
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.databinding.FragmentProfileManagerBinding
import com.cosmos.unreddit.ui.common.CarouselPageTransformer
import com.cosmos.unreddit.ui.common.ProfileNameDialog
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileManagerDialogFragment : DialogFragment(), ProfileManagerAdapter.ProfileClickListener {

    private var _binding: FragmentProfileManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileManagerViewModel by viewModels()

    private lateinit var profileAdapter: ProfileManagerAdapter

    private var currentProfile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentProfile = arguments?.parcelable(KEY_CURRENT_PROFILE)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profiles
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { profiles ->
                    profileAdapter.submitList(profiles)
                    profiles.indexOfFirst { item ->
                        (item as? ProfileItem.UserProfile)?.profile?.id == currentProfile?.id
                    }.let { index ->
                        binding.viewPager.currentItem = index
                    }
                }
        }
    }

    private fun showAddProfileDialog() {
        ProfileNameDialog.show(
            fragment = this,
            title = R.string.dialog_create_profile_title,
            positiveButton = R.string.dialog_create_profile_button,
            existingNames = existingProfileNames()
        ) { name ->
            viewModel.addProfile(name)
        }
    }

    private fun showRenameProfileDialog(profile: Profile) {
        ProfileNameDialog.show(
            fragment = this,
            title = R.string.dialog_rename_profile_title,
            positiveButton = R.string.dialog_rename_profile_button,
            existingNames = existingProfileNames(),
            initialName = profile.name
        ) { name ->
            viewModel.renameProfile(profile, name)
        }
    }

    private fun existingProfileNames(): List<String> {
        return profileAdapter.currentList
            .filterIsInstance<ProfileItem.UserProfile>()
            .map { it.profile.name }
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

    override fun onRenameClick(profile: Profile) {
        showRenameProfileDialog(profile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileManagerDialogFragment"

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
