package com.cosmos.unreddit.ui.postlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentPostBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.extension.applyMarginWindowInsets
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
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
class PostListFragment : BaseFragment() {

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

    private lateinit var postListAdapter: PostListAdapter

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar.root) { appBar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            appBar.updateLayoutParams<AppBarLayout.LayoutParams> {
                topMargin = insets.top
            }

            windowInsets
        }

        initResultListener()
        initAppBar()
        initRecyclerView()
        bindViewModel()
        binding.infoRetry.apply {
            applyMarginWindowInsets(left = false, right = false, bottom = false)
            setActionClickListener { postListAdapter.retry() }
        }
    }

    override fun applyInsets(view: View) {
        // ignore
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
        }
    }

    private fun initRecyclerView() {
        postListAdapter = PostListAdapter(repository, this, this).apply {
            addLoadStateListener { loadState ->
                binding.listPost.isVisible = loadState.source.refresh is LoadState.NotLoading

                binding.loadingCradle.isVisible = loadState.source.refresh is LoadState.Loading

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

        launchRepeat(Lifecycle.State.STARTED) {
            postListAdapter.onRefreshFromNetwork {
                scrollToTop()
            }
        }
    }

    private fun initAppBar() {
        binding.appBar.sortCard.setOnClickListener { showSortDialog() }
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

    override fun onBackPressed() {
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PostListFragment"
    }
}
