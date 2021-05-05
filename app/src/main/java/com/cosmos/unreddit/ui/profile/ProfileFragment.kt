package com.cosmos.unreddit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.databinding.FragmentProfileBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.commentmenu.CommentMenuFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.user.UserCommentsAdapter
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.scrollToTop
import com.cosmos.unreddit.util.extension.setCommentListener
import com.cosmos.unreddit.util.extension.setNavigationListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), UserCommentsAdapter.CommentClickListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    override val viewModel: ProfileViewModel by viewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private lateinit var savedAdapter: ProfileSavedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initResultListener()
        initViewPager()
        bindViewModel()
    }

    private fun initResultListener() {
        setCommentListener { comment -> comment?.let { viewModel.toggleSaveComment(it) } }

        setNavigationListener { showNavigation ->
            uiViewModel.setNavigationVisibility(showNavigation)
        }
    }

    private fun initViewPager() {
        savedAdapter = ProfileSavedAdapter(requireContext(), this, this, this)

        val tabs: List<RecyclerViewStateAdapter.Page> = listOf(
            RecyclerViewStateAdapter.Page(R.string.tab_profile_saved, savedAdapter),
        )

        val userStateAdapter = RecyclerViewStateAdapter {
            // TODO
        }.apply {
            submitList(tabs)
        }

        binding.viewPager.apply {
            adapter = userStateAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.setPage(position)
                }
            })
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // ignore
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // ignore
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.let { binding.viewPager.scrollToTop(it.position) }
            }
        })

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.setText(tabs[position].title)
        }.attach()
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(viewModel.savedItems, viewModel.contentPreferences) { items, preferences ->
                savedAdapter.run {
                    contentPreferences = preferences
                    submitList(items)
                }
            }.collect()
        }
        viewModel.currentProfile.asLiveData().observe(viewLifecycleOwner) {
            binding.profile = it
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
