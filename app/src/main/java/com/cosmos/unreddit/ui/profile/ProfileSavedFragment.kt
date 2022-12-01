package com.cosmos.unreddit.ui.profile

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentTransaction
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.ui.commentmenu.CommentMenuFragment
import com.cosmos.unreddit.ui.common.fragment.ListFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.user.UserCommentsAdapter
import com.cosmos.unreddit.util.extension.currentNavigationFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileSavedFragment : ListFragment<ProfileSavedAdapter>(),
    UserCommentsAdapter.CommentClickListener {

    override val viewModel: ProfileViewModel by hiltNavGraphViewModels(R.id.profile)

    override val enablePullToRefresh: Boolean
        get() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContentView()
        bindViewModel()
    }

    private fun updateContentView() {
        // Update empty data view to be higher than usual
        val contentMargin = resources.getDimension(R.dimen.profile_content_margin).toInt()

        binding.loadingState.run {
            ConstraintSet().apply {
                clone(root)
                clear(textEmptyData.id, ConstraintSet.BOTTOM)
                applyTo(root)
            }

            emptyData.updateLayoutParams<MarginLayoutParams> { topMargin = contentMargin }
        }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.savedItems, viewModel.contentPreferences) { items, preferences ->
                adapter.run {
                    contentPreferences = preferences
                    submitList(items)
                    binding.loadingState.run {
                        emptyData.isVisible = items.isEmpty()
                        textEmptyData.isVisible = items.isEmpty()
                    }
                }
            }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect()
        }
    }

    override fun onClick(comment: Comment.CommentEntity) {
        activity?.currentNavigationFragment
            ?.parentFragmentManager
            ?.beginTransaction()
            ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            ?.add(
                R.id.fragment_container,
                PostDetailsFragment.newInstance(comment.permalink),
                PostDetailsFragment.TAG
            )
            ?.addToBackStack(null)
            ?.commit()
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
}
