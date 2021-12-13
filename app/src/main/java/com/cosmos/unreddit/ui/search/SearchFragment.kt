package com.cosmos.unreddit.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.NavigationGraphDirections
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentSearchBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.SearchUtil
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import com.cosmos.unreddit.util.extension.scrollToTop
import com.cosmos.unreddit.util.extension.setSortingListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : BaseFragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override val viewModel: SearchViewModel by viewModels()

    private val args: SearchFragmentArgs by navArgs()

    private lateinit var postListAdapter: PostListAdapter
    private lateinit var subredditAdapter: SearchSubredditAdapter
    private lateinit var userAdapter: SearchUserAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val query = args.query

            viewModel.setQuery(query)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val query = viewModel.query.value.takeIf { it.isNotBlank() } ?: args.query

        binding.appBar.searchInput.setText(query)

        initResultListener()
        initAppBar()
        initViewPager()
        bindViewModel()

        binding.infoRetry.setActionClickListener { retry() }

        lifecycleScope.launch {
            delay(250)
            showSearchInput(false)
        }
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewModel.query.collect { query ->
                    query.takeIf { it.isNotBlank() }?.let {
                        binding.appBar.label.text = query
                    }
                }
            }

            launch {
                viewModel.contentPreferences.collect {
                    postListAdapter.contentPreferences = it
                }
            }

            launch {
                viewModel.sorting.collect {
                    binding.appBar.sortIcon.setSorting(it)
                }
            }

            launch {
                viewModel.postDataFlow.collectLatest {
                    postListAdapter.submitData(it)
                }
            }

            launch {
                viewModel.subredditDataFlow.collectLatest {
                    subredditAdapter.submitData(it)
                }
            }

            launch {
                viewModel.userDataFlow.collectLatest {
                    userAdapter.submitData(it)
                }
            }
        }
    }

    private fun initViewPager() {
        postListAdapter = PostListAdapter(repository, this, this)
        subredditAdapter = SearchSubredditAdapter { onSubredditClick(it) }
        userAdapter = SearchUserAdapter { onUserClick(it) }

        val tabs: List<RecyclerViewStateAdapter.Page> = listOf(
            RecyclerViewStateAdapter.Page(R.string.tab_search_post, postListAdapter, true),
            RecyclerViewStateAdapter.Page(R.string.tab_search_subreddit, subredditAdapter),
            RecyclerViewStateAdapter.Page(R.string.tab_search_user, userAdapter)
        )

        val searchStateAdapter = RecyclerViewStateAdapter {
            showRetryBar()
        }.apply {
            submitList(tabs)
        }

        binding.viewPager.apply {
            adapter = searchStateAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // TODO: Back to top when reselected
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

        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                postListAdapter.onRefreshFromNetwork {
                    binding.viewPager.scrollToTop(0)
                }
            }

            launch {
                subredditAdapter.onRefreshFromNetwork {
                    binding.viewPager.scrollToTop(1)
                }
            }

            launch {
                userAdapter.onRefreshFromNetwork {
                    binding.viewPager.scrollToTop(2)
                }
            }
        }
    }

    private fun initAppBar() {
        with(binding.appBar) {
            label.setOnClickListener { showSearchInput(true) }
            root.setOnClickListener { showSearchInput(true) }
            searchInput.apply {
                addTarget(backCard)
                addTarget(label)
                addTarget(sortCard)
                addTarget(sortIcon)
                addTarget(cancelCard)
                setSearchActionListener {
                    handleSearchAction(it)
                }
            }
            sortCard.setOnClickListener { showSortDialog() }
            backCard.setOnClickListener { onBackPressed() }
            cancelCard.setOnClickListener { showSearchInput(false) }
        }
    }

    private fun initResultListener() {
        setSortingListener { sorting -> sorting?.let { viewModel.setSorting(it) } }
    }

    private fun showSearchInput(show: Boolean) {
        binding.appBar.searchInput.apply {
            show(binding.appBar.root, show) {
                with(binding.appBar) {
                    backCard.isVisible = !show
                    label.isVisible = !show
                    sortCard.isVisible = !show
                    sortIcon.isVisible = !show
                    cancelCard.isVisible = show
                }
            }
            setSelection(text?.length ?: 0)
        }
    }

    private fun handleSearchAction(query: String) {
        if (SearchUtil.isQueryValid(query)) {
            viewModel.setQuery(query)
            showSearchInput(false)
        }
    }

    private fun onSubredditClick(subreddit: String) {
        navigate(NavigationGraphDirections.openSubreddit(subreddit))
    }

    private fun onUserClick(user: String) {
        navigate(NavigationGraphDirections.openUser(user))
    }

    private fun retry() {
        // TODO: Don't retry if not necessary
        postListAdapter.retry()
        subredditAdapter.retry()
        userAdapter.retry()
    }

    private fun showRetryBar() {
        if (!binding.infoRetry.isVisible) {
            binding.infoRetry.show()
        }
    }

    private fun showSortDialog() {
        SortFragment.show(
            childFragmentManager,
            viewModel.sorting.value,
            SortFragment.SortType.SEARCH
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "SearchFragment"
    }
}
