package com.cosmos.unreddit.ui.postlist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentPostBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.common.widget.PullToRefreshLayout
import com.cosmos.unreddit.ui.common.widget.PullToRefreshView
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.extension.applyMarginWindowInsets
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.clearNavigationListener
import com.cosmos.unreddit.util.extension.clearSortingListener
import com.cosmos.unreddit.util.extension.clearWindowInsetsListener
import com.cosmos.unreddit.util.extension.getFloatValue
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import com.cosmos.unreddit.util.extension.setNavigationListener
import com.cosmos.unreddit.util.extension.setSortingListener
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PostListFragment : BaseFragment(), PullToRefreshLayout.OnRefreshListener {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    override val viewModel: PostListViewModel by activityViewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    // Workaround for nested CoordinatorLayout that prevents bottom navigation from being hidden on
    // scroll
    private val onOffsetChangedListener = object : AppBarLayout.OnOffsetChangedListener {
        var visible: Boolean = true
            private set

        override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
            if (verticalOffset != 0 && visible) {
                visible = false
                uiViewModel.setNavigationVisibility(false)
            } else if (verticalOffset == 0 && !visible) {
                visible = true
                uiViewModel.setNavigationVisibility(true)
            }
        }
    }

    private val contentScale by lazy { resources.getFloatValue(R.dimen.subreddit_content_scale) }
    private val contentRadius by lazy { resources.getDimension(R.dimen.subreddit_content_radius) }
    private val contentElevation by lazy {
        resources.getDimension(R.dimen.subreddit_content_elevation)
    }

    private val isDrawerOpen: Boolean
        get() = binding.drawerLayout.isDrawerOpen(GravityCompat.START)

    private lateinit var postListAdapter: PostListAdapter

    private lateinit var profileAdapter: ProfileAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initRecyclerView()
        initDrawer()
        bindViewModel()

        binding.infoRetry.apply {
            applyMarginWindowInsets(left = false, right = false, bottom = false)
            setActionClickListener { postListAdapter.retry() }
        }
    }

    override fun onStart() {
        super.onStart()
        initResultListener()
    }

    override fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { rootView, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appBar.root.updateLayoutParams<AppBarLayout.LayoutParams> {
                topMargin = insets.top
            }

            binding.listProfiles.run {
                updatePadding(
                    paddingLeft,
                    insets.top,
                    paddingRight,
                    paddingBottom
                )
            }

            rootView.clearWindowInsetsListener()

            windowInsets
        }
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewModel.contentPreferences.collect {
                    binding.infoRetry.hide()
                    postListAdapter.contentPreferences = it
                }
            }

            launch {
                viewModel.profiles.collect {
                    profileAdapter.submitList(it)
                }
            }

            launch {
                viewModel.fetchData.collect {
                    binding.infoRetry.hide()
                }
            }

            launch {
                viewModel.postDataFlow.collectLatest {
                    postListAdapter.submitData(it)
                }
            }

            launch {
                viewModel.sorting.collect {
                    binding.appBar.sortIcon.setSorting(it)
                }
            }

            launch {
                viewModel.currentProfile.collect {
                    binding.appBar.profileImage.setText(it.name)
                }
            }
            
            launch { 
                viewModel.lastRefresh.collect {
                    val time = getString(R.string.last_refresh, DateUtil.getLocalizedTime(it))
                    (binding.pullRefresh.refreshView as? PullToRefreshView)?.setLastRefresh(time)
                }
            }
        }
    }

    private fun initDrawer() {
        binding.drawerLayout.apply {
            setScrimColor(Color.TRANSPARENT)
            drawerElevation = 0F
            addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    val slideX = drawerView.width * slideOffset
                    val scale = 1 - (slideOffset / (contentScale * SCALE_FACTOR))
                    updateContainerView(
                        slideX,
                        scale,
                        slideOffset * contentElevation,
                        slideOffset * contentRadius
                    )
                }
            })
        }

        profileAdapter = ProfileAdapter { onProfileClick(it) }

        binding.listProfiles.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = profileAdapter
        }

        // Restore container view when drawer was open before
        if (viewModel.isDrawerOpen) {
            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val scale = 1 - (1 / (contentScale * SCALE_FACTOR))
                    updateContainerView(
                        binding.navigationView.width.toFloat(),
                        scale,
                        contentElevation,
                        contentRadius
                    )
                    binding.navigationView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
            binding.navigationView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }
    }

    private fun initRecyclerView() {
        postListAdapter = PostListAdapter(repository, this, this).apply {
            addLoadStateListener { loadState ->
                val isLoading = loadState.source.refresh is LoadState.Loading

                binding.run {
                    if (!pullRefresh.isRefreshing) {
                        listPost.isVisible = loadState.source.refresh is LoadState.NotLoading

                        loadingCradle.isVisible = isLoading
                    } else {
                        pullRefresh.setRefreshing(isLoading)
                    }
                }

                val errorState = loadState.source.refresh as? LoadState.Error
                errorState?.let {
                    binding.infoRetry.show()
                }
            }
        }

        binding.listPost.apply {
            applyWindowInsets(left = false, top = false, right = false)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postListAdapter.withLoadStateHeaderAndFooter(
                header = NetworkLoadStateAdapter { postListAdapter.retry() },
                footer = NetworkLoadStateAdapter { postListAdapter.retry() }
            )
        }

        binding.pullRefresh.setOnRefreshListener(this)

        launchRepeat(Lifecycle.State.STARTED) {
            postListAdapter.onRefreshFromNetwork {
                scrollToTop()
            }
        }
    }

    private fun initAppBar() {
        binding.appBar.run {
            sortCard.setOnClickListener { showSortDialog() }
            profileImage.setOnClickListener { openProfileDrawer() }
            title.setOnClickListener { scrollToTop() }
        }
        binding.appBarLayout.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    private fun initResultListener() {
        setSortingListener { sorting -> sorting?.let { viewModel.setSorting(it) } }

        setNavigationListener { showNavigation ->
            uiViewModel.setNavigationVisibility(showNavigation && onOffsetChangedListener.visible)
        }
    }

    fun scrollToTop() {
        binding.listPost.betterSmoothScrollToPosition(0)
    }

    private fun showSortDialog() {
        SortFragment.show(childFragmentManager, viewModel.sorting.value)
    }

    private fun updateContainerView(
        translationX: Float,
        scale: Float,
        elevation: Float,
        radius: Float
    ) {
        binding.container.apply {
            this.translationX = translationX
            this.scaleX = scale
            this.scaleY = scale
            this.cardElevation = elevation
            this.radius = radius
        }
    }

    private fun openProfileDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeProfileDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun onProfileClick(profile: Profile) {
        viewModel.selectProfile(profile)
        closeProfileDrawer()
        // Show app bar on profile change to prevent weird scrolling behaviors
        binding.appBarLayout.setExpanded(true)
    }

    override fun onRefresh() {
        postListAdapter.refresh()
    }

    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeProfileDrawer()
        } else {
            activity?.finish()
        }
    }

    override fun onStop() {
        super.onStop()
        clearSortingListener()
        clearNavigationListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        (binding.pullRefresh.refreshView as? PullToRefreshLayout.RefreshCallback)?.reset()

        viewModel.isDrawerOpen = isDrawerOpen

        _binding = null
    }

    companion object {
        const val TAG = "PostListFragment"

        private const val SCALE_FACTOR = 10
    }
}
