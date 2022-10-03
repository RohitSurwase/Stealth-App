package com.cosmos.unreddit.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.ui.commentmenu.CommentMenuFragment
import com.cosmos.unreddit.ui.common.fragment.ListFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment.Companion.REQUEST_KEY_NAVIGATION
import com.cosmos.unreddit.ui.user.UserCommentsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileSavedFragment : ListFragment<ProfileSavedAdapter>(),
    UserCommentsAdapter.CommentClickListener {

    override val viewModel: ProfileViewModel by hiltNavGraphViewModels(R.id.profile)
    private val uiViewModel: UiViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initResultListener()
        bindViewModel()
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.savedItems, viewModel.contentPreferences) { items, preferences ->
                adapter.run {
                    contentPreferences = preferences
                    submitList(items)
                }
            }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect()
        }
    }

    private fun initResultListener() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            REQUEST_KEY_NAVIGATION,
            this
        ) { _, bundle ->
            val showNavigation = bundle.getBoolean(PostDetailsFragment.BUNDLE_KEY_NAVIGATION)
            uiViewModel.setNavigationVisibility(showNavigation)
        }
    }

    override fun onClick(comment: Comment.CommentEntity) {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(
                R.id.fragment_container,
                PostDetailsFragment.newInstance(comment.permalink),
                PostDetailsFragment.TAG
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onLongClick(comment: Comment.CommentEntity) {
        CommentMenuFragment.show(
            childFragmentManager,
            comment,
            CommentMenuFragment.MenuType.DETAILS
        )
    }

    override fun createAdapter(): ProfileSavedAdapter {
        return ProfileSavedAdapter(requireContext(), this, this, this)
    }

    override fun onDestroyView() {
        requireActivity().supportFragmentManager.clearFragmentResultListener(REQUEST_KEY_NAVIGATION)
        super.onDestroyView()
    }
}
