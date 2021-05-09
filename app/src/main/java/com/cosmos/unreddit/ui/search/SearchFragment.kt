package com.cosmos.unreddit.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.SubscriptionsDirections
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentSearchBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import com.cosmos.unreddit.util.extension.scrollToTop
import com.cosmos.unreddit.util.extension.setSortingListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : BaseFragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override val viewModel: SearchViewModel by viewModels()

    private val args: SearchFragmentArgs by navArgs()

    private var searchPostJob: Job? = null
    private var searchSubredditJob: Job? = null
    private var searchUserJob: Job? = null

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
        val query = viewModel.query.value ?: args.query

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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.query.collectLatest { query ->
                query?.let {
                    binding.appBar.label.text = query
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.query,
                viewModel.sorting,
                viewModel.page,
                viewModel.contentPreferences
            ) { query, sorting, page, contentPreferences ->
                postListAdapter.contentPreferences = contentPreferences
                query?.let {
                    search(page, query, sorting)
                }
                binding.appBar.sortIcon.setSorting(sorting)
            }.collect()
        }
    }

    private fun initViewPager() {
        postListAdapter = PostListAdapter(repository, this, this)
        subredditAdapter = SearchSubredditAdapter { onSubredditClick(it) }
        userAdapter = SearchUserAdapter { onUserClick(it) }

        val tabs: List<RecyclerViewStateAdapter.Page> = listOf(
            RecyclerViewStateAdapter.Page(R.string.tab_search_post, postListAdapter),
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

        lifecycleScope.launchWhenStarted {
            postListAdapter.onRefreshFromNetwork {
                binding.viewPager.scrollToTop(0)
            }
        }
        lifecycleScope.launchWhenStarted {
            subredditAdapter.onRefreshFromNetwork {
                binding.viewPager.scrollToTop(1)
            }
        }
        lifecycleScope.launchWhenStarted {
            userAdapter.onRefreshFromNetwork {
                binding.viewPager.scrollToTop(2)
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
                setOnEditorActionListener { _, actionId, _ ->
                    when (actionId) {
                        EditorInfo.IME_ACTION_SEARCH -> {
                            if (text.toString().length >= QUERY_MIN_LENGTH) {
                                viewModel.setQuery(text.toString())
                                showSearchInput(false)
                            }
                            true
                        }
                        else -> false
                    }
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

    private fun search(position: Int, query: String, sorting: Sorting) {
        when (position) {
            0 -> searchPost(query, sorting)
            1 -> searchSubreddit(query, sorting)
            2 -> searchUser(query, sorting)
        }
    }

    private fun searchPost(query: String, sorting: Sorting) {
        searchPostJob?.cancel()
        searchPostJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchAndFilterPosts(query, sorting).collectLatest {
                postListAdapter.submitData(it)
            }
        }
    }

    private fun searchSubreddit(query: String, sorting: Sorting) {
        searchSubredditJob?.cancel()
        searchSubredditJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchAndFilterSubreddits(query, sorting).collectLatest {
                subredditAdapter.submitData(it)
            }
        }
    }

    private fun searchUser(query: String, sorting: Sorting) {
        searchUserJob?.cancel()
        searchUserJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchAndFilterUsers(query, sorting).collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    private fun onSubredditClick(subreddit: String) {
        navigate(SubscriptionsDirections.openSubreddit(subreddit))
    }

    private fun onUserClick(user: String) {
        navigate(SearchFragmentDirections.openUser(user))
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

        const val QUERY_MIN_LENGTH = 3
    }
}
