package com.cosmos.unreddit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.databinding.FragmentProfileBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.common.adapter.FragmentAdapter
import com.cosmos.unreddit.ui.profilemanager.ProfileManagerDialogFragment
import com.cosmos.unreddit.util.extension.clearCommentListener
import com.cosmos.unreddit.util.extension.clearNavigationListener
import com.cosmos.unreddit.util.extension.getListContent
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.latest
import com.cosmos.unreddit.util.extension.scrollToTop
import com.cosmos.unreddit.util.extension.setCommentListener
import com.cosmos.unreddit.util.extension.setNavigationListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    override val viewModel: ProfileViewModel by hiltNavGraphViewModels(R.id.profile)
    private val uiViewModel: UiViewModel by activityViewModels()

    // Workaround for MotionLayout that prevents bottom navigation from being hidden on scroll
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0 && uiViewModel.navigationVisibility.value) {
                uiViewModel.setNavigationVisibility(false)
            } else if (dy < 0 && !uiViewModel.navigationVisibility.value) {
                uiViewModel.setNavigationVisibility(true)
            }
        }
    }

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
        initAppBar()
        initViewPager()
        bindViewModel()

        viewModel.layoutState?.let { binding.layoutRoot.jumpToState(it) }
    }

    private fun initResultListener() {
        setCommentListener { comment -> comment?.let { viewModel.toggleSaveComment(it) } }

        setNavigationListener { showNavigation ->
            uiViewModel.setNavigationVisibility(showNavigation)
        }
    }

    private fun initAppBar() {
        binding.usersCard.setOnClickListener {
            lifecycleScope.launch {
                viewModel.currentProfile.latest?.let {
                    ProfileManagerDialogFragment.show(parentFragmentManager, it)
                }
            }
        }
    }

    private fun initViewPager() {
        val fragments = listOf(
            FragmentAdapter.Page(R.string.tab_profile_saved, ProfileSavedFragment::class.java)
        )

        val fragmentAdapter = FragmentAdapter(this, fragments)

        binding.viewPager.apply {
            adapter = fragmentAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.setPage(position)
                    registerScrollListener(position)
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
            tab.setText(fragments[position].title)
        }.attach()
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProfile
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect {
                    binding.profile = it
                }
        }
    }

    private fun registerScrollListener(position: Int) {
        binding.viewPager.getListContent(position)?.let {
            it.listContent.run {
                clearOnScrollListeners()
                addOnScrollListener(onScrollListener)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.viewPager.adapter?.let {
            for (i in 0 until it.itemCount) {
                binding.viewPager.getListContent(i)?.listContent?.clearOnScrollListeners()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Save header state to restore it in case of fragment recreation
        viewModel.layoutState = binding.layoutRoot.currentState

        clearCommentListener()
        clearNavigationListener()

        _binding = null
    }
}
