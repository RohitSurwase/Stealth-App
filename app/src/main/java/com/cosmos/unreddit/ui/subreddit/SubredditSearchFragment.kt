package com.cosmos.unreddit.ui.subreddit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentSubredditSearchBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.SearchUtil
import com.cosmos.unreddit.util.extension.addLoadStateListener
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.hideSoftKeyboard
import com.cosmos.unreddit.util.extension.loadSubredditIcon
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
class SubredditSearchFragment : BaseFragment(), PostListAdapter.PostClickListener {

    private var _binding: FragmentSubredditSearchBinding? = null
    private val binding get() = _binding!!

    override val viewModel: SubredditSearchViewModel by viewModels()

    private val args: SubredditSearchFragmentArgs by navArgs()

    private var searchPostJob: Job? = null

    private lateinit var postListAdapter: PostListAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setSubreddit(args.subreddit)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubredditSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initResultListener()
        initAppBar()
        initRecyclerView()
        bindViewModel()
        binding.loadingState.infoRetry.setActionClickListener { postListAdapter.retry() }
    }

    override fun onResume() {
        super.onResume()
        if (binding.appBar.searchInput.isQueryEmpty()) {
            showSearchInput(true)
        } else {
            binding.appBar.apply {
                searchInput.isVisible = false
                cancelCard.isVisible = false
            }
        }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.query.collectLatest { query ->
                query?.let {
                    binding.appBar.label.text = query
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.subreddit.collectLatest { subreddit ->
                subreddit?.let {
                    binding.appBar.searchInput.hint = getString(R.string.search_hint_subreddit, it)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.subreddit,
                viewModel.query,
                viewModel.sorting,
                viewModel.contentPreferences
            ) { subreddit, query, sorting, contentPreferences ->
                binding.loadingState.infoRetry.hide()
                postListAdapter.contentPreferences = contentPreferences
                if (subreddit != null && query != null) {
                    searchPost(query, sorting)
                }
                binding.appBar.sortIcon.setSorting(sorting)
            }.collect()
        }
    }

    private fun initRecyclerView() {
        postListAdapter = PostListAdapter(repository, this, this).apply {
            addLoadStateListener(binding.listPost, binding.loadingState) {
                showRetryBar()
            }
        }

        with(binding.listPost) {
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
        with(binding.appBar) {
            subredditImage.loadSubredditIcon(args.icon)
            sortCard.setOnClickListener { showSortDialog() }
            cancelCard.setOnClickListener { cancelSearch() }
            backCard.setOnClickListener { activity?.onBackPressed() }
            label.setOnClickListener { showSearchInput(true) }
            root.setOnClickListener { showSearchInput(true) }
            searchInput.apply {
                addTarget(backCard)
                addTarget(subredditImage)
                addTarget(label)
                addTarget(sortIcon)
                addTarget(sortCard)
                addTarget(cancelCard)
                setSearchActionListener {
                    handleSearchAction(it)
                }
            }
        }
    }

    private fun initResultListener() {
        setSortingListener { sorting -> sorting?.let { viewModel.setSorting(it) } }
    }

    private fun scrollToTop() {
        binding.listPost.betterSmoothScrollToPosition(0)
    }

    private fun searchPost(query: String, sorting: Sorting) {
        searchPostJob?.cancel()
        searchPostJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchAndFilterPosts(query, sorting).collectLatest {
                postListAdapter.submitData(it)
            }
        }
    }

    private fun showSearchInput(show: Boolean) {
        binding.appBar.searchInput.show(binding.appBar.root, show) {
            with(binding.appBar) {
                backCard.isVisible = !show
                label.isVisible = !show
                sortCard.isVisible = !show
                sortIcon.isVisible = !show
                subredditImage.isVisible = !show
                cancelCard.isVisible = show
            }
        }
    }

    private fun showSortDialog() {
        SortFragment.show(
            childFragmentManager,
            viewModel.sorting.value,
            SortFragment.SortType.SEARCH
        )
    }

    private fun showRetryBar() {
        if (!binding.loadingState.infoRetry.isVisible) {
            binding.loadingState.infoRetry.show()
        }
    }

    private fun cancelSearch() {
        if (viewModel.query.value != null) {
            showSearchInput(false)
        } else {
            binding.appBar.searchInput.hideSoftKeyboard()
            activity?.onBackPressed()
        }
    }

    private fun handleSearchAction(query: String) {
        if (SearchUtil.isQueryValid(query)) {
            viewModel.setQuery(query)
            showSearchInput(false)
        }
    }

    override fun onBackPressed() {
        if (binding.appBar.searchInput.isVisible && viewModel.query.value != null) {
            showSearchInput(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SubredditSearchFragment"
    }
}
