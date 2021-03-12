package com.cosmos.unreddit.postlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentPostBinding
import com.cosmos.unreddit.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postdetails.PostDetailsFragment
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.setSortingListener
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

    private val viewModel: PostListViewModel by activityViewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private var loadPostsJob: Job? = null

    private lateinit var adapter: PostListAdapter

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
        binding.infoRetry.setActionClickListener { adapter.retry() }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.subreddit,
                viewModel.sorting,
                viewModel.contentPreferences
            ) { subreddit, sorting, contentPreferences ->
                binding.infoRetry.hide()
                adapter.setContentPreferences(contentPreferences)
                loadPosts(subreddit, sorting)
                setSortIcon(sorting)
            }.collect() // TODO: Sometimes does not scroll to top
        }
    }

    private fun initRecyclerView() {
        adapter = PostListAdapter(repository, this, this).apply {
            addLoadStateListener { loadState ->
                binding.listPost.isVisible = loadState.source.refresh is LoadState.NotLoading

                binding.loadingCradle.isVisible = loadState.source.refresh is LoadState.Loading

                val errorState = loadState.source.refresh as? LoadState.Error
                errorState?.let {
                    binding.infoRetry.show()
                }
            }
        }
        binding.listPost.layoutManager = LinearLayoutManager(requireContext())
        binding.listPost.adapter = adapter.withLoadStateHeaderAndFooter(
            header = NetworkLoadStateAdapter { adapter.retry() },
            footer = NetworkLoadStateAdapter { adapter.retry() }
        )
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

    private fun setSortIcon(sorting: Sorting) {
        val popInAnimation = AnimationUtils.loadAnimation(context, R.anim.pop_in)
        val popOutAnimation = AnimationUtils.loadAnimation(context, R.anim.pop_out)

        with(binding.appBar.sortIcon) {
            visibility = if (sorting.generalSorting == RedditApi.Sort.HOT) {
                View.GONE
            } else {
                View.VISIBLE
            }

            when (sorting.generalSorting) {
                RedditApi.Sort.NEW -> setImageResource(R.drawable.ic_new)
                RedditApi.Sort.TOP -> setImageResource(R.drawable.ic_top)
                RedditApi.Sort.RISING -> setImageResource(R.drawable.ic_rising)
                RedditApi.Sort.CONTROVERSIAL -> setImageResource(R.drawable.ic_controversial)
                else -> {
                    startAnimation(popOutAnimation)
                    return@with
                }
            }

            startAnimation(popInAnimation)
        }

        with(binding.appBar.sortTimeText) {
            val showOutAnimation = isVisible

            visibility = if (sorting.generalSorting == RedditApi.Sort.TOP ||
                sorting.generalSorting == RedditApi.Sort.CONTROVERSIAL
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

            text = when (sorting.timeSorting) {
                RedditApi.TimeSorting.HOUR -> getString(R.string.sort_time_hour_short)
                RedditApi.TimeSorting.DAY -> getString(R.string.sort_time_day_short)
                RedditApi.TimeSorting.WEEK -> getString(R.string.sort_time_week_short)
                RedditApi.TimeSorting.MONTH -> getString(R.string.sort_time_month_short)
                RedditApi.TimeSorting.YEAR -> getString(R.string.sort_time_year_short)
                RedditApi.TimeSorting.ALL -> getString(R.string.sort_time_all_short)
                null -> {
                    if (showOutAnimation) startAnimation(popOutAnimation)
                    return@with
                }
            }

            startAnimation(popInAnimation)
        }
    }

    private fun loadPosts(subreddit: String, sorting: Sorting) {
        loadPostsJob?.cancel()
        loadPostsJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAndFilterPosts(subreddit, sorting).collectLatest {
                adapter.submitData(it)
            }
        }
    }

    fun scrollToTop() {
        // TODO: Find better method when item is too far
        binding.listPost.smoothScrollToPosition(0)
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

        @JvmStatic
        fun newInstance() = PostListFragment()
    }
}
