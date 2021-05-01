package com.cosmos.unreddit.ui.postlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentPostBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import com.cosmos.unreddit.util.extension.setSortingListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PostListFragment : BaseFragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    override val viewModel: PostListViewModel by activityViewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private var loadPostsJob: Job? = null

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
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.postListFragment -> uiViewModel.setNavigationVisibility(true)
                else -> uiViewModel.setNavigationVisibility(false)
            }
        }
        initResultListener()
        initAppBar()
        initRecyclerView()
        bindViewModel()
        binding.infoRetry.setActionClickListener { postListAdapter.retry() }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.subreddit,
                viewModel.sorting,
                viewModel.contentPreferences
            ) { subreddit, sorting, contentPreferences ->
                binding.infoRetry.hide()
                postListAdapter.contentPreferences = contentPreferences
                loadPosts(subreddit, sorting)
            }.collect()
        }
        viewModel.sorting.asLiveData().observe(viewLifecycleOwner) {
            binding.appBar.sortIcon.setSorting(it)
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
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postListAdapter.withLoadStateHeaderAndFooter(
                header = NetworkLoadStateAdapter { postListAdapter.retry() },
                footer = NetworkLoadStateAdapter { postListAdapter.retry() }
            )
        }

        lifecycleScope.launchWhenStarted {
            postListAdapter.onRefreshFromNetwork {
                scrollToTop()
            }
        }
    }

    private fun initAppBar() {
        binding.appBar.sortCard.setOnClickListener { showSortDialog() }
    }

    private fun initResultListener() {
        setSortingListener { sorting -> sorting?.let { viewModel.setSorting(it) } }

        setFragmentResultListener(PostDetailsFragment.REQUEST_KEY_NAVIGATION) { _, bundle ->
            val showNavigation = bundle.getBoolean(PostDetailsFragment.BUNDLE_KEY_NAVIGATION)
            uiViewModel.setNavigationVisibility(showNavigation)
        }
    }

    private fun loadPosts(subreddit: String, sorting: Sorting) {
        loadPostsJob?.cancel()
        loadPostsJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAndFilterPosts(subreddit, sorting).collectLatest {
                postListAdapter.submitData(it)
            }
        }
    }

    fun scrollToTop() {
        binding.listPost.betterSmoothScrollToPosition(0)
    }

    private fun showSortDialog() {
        SortFragment.show(childFragmentManager, viewModel.sorting.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PostListFragment"
    }
}
