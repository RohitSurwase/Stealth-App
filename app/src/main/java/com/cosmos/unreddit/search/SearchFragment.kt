package com.cosmos.unreddit.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentSearchBinding
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListAdapter
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.RedditUri
import com.cosmos.unreddit.util.hideSoftKeyboard
import com.cosmos.unreddit.util.setSortingListener
import com.cosmos.unreddit.util.showSoftKeyboard
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialFadeThrough
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

    private val viewModel: SearchViewModel by viewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private val args: SearchFragmentArgs by navArgs()

    private var searchPostJob: Job? = null
    private var searchSubredditJob: Job? = null
    private var searchUserJob: Job? = null

    private lateinit var postListAdapter: PostListAdapter
    private lateinit var subredditAdapter: SearchSubredditAdapter
    private lateinit var userAdapter: SearchUserAdapter

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @Inject
    lateinit var repository: PostListRepository

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
        uiViewModel.setNavigationVisibility(false)

        val query = args.query

        binding.appBar.searchInput.setText(query)
        viewModel.setQuery(query)

        initResultListener()
        initAppBar()
        initViewPager()
        bindViewModel()

        lifecycleScope.launch {
            delay(250)
            showSearchInput(false)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.appBar.searchInput.isVisible) {
                    showSearchInput(false)
                } else {
                    uiViewModel.setNavigationVisibility(true)
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(this, onBackPressedCallback)
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
            // TODO: Back to top when query changed
            combine(
                viewModel.query,
                viewModel.sorting,
                viewModel.page,
                viewModel.contentPreferences
            ) { query, sorting, page, contentPreferences ->
                postListAdapter.setContentPreferences(contentPreferences)
                query?.let {
                    search(page, query, sorting)
                }
                setSortIcon(sorting)
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

        val searchStateAdapter = RecyclerViewStateAdapter().apply { submitList(tabs) }
        binding.viewPager.apply {
            adapter = searchStateAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // TODO: Back to top when reselected
                    viewModel.setPage(position)
                }
            })
        }

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.setText(tabs[position].title)
        }.attach()
    }

    private fun initAppBar() {
        with(binding.appBar) {
            label.setOnClickListener {
                showSearchInput(true)
            }
            root.setOnClickListener {
                showSearchInput(true)
            }
            searchInput.apply {
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
            backCard.setOnClickListener { requireActivity().onBackPressed() }
        }
    }

    private fun initResultListener() {
        setSortingListener { sorting -> sorting?.let { viewModel.setSorting(it) } }
    }

    private fun setSortIcon(sorting: Sorting) {
        val popInAnimation = AnimationUtils.loadAnimation(context, R.anim.pop_in)
        val popOutAnimation = AnimationUtils.loadAnimation(context, R.anim.pop_out)

        with(binding.appBar.sortIcon) {
            when (sorting.generalSorting) {
                RedditApi.Sort.HOT -> setImageResource(R.drawable.ic_hot)
                RedditApi.Sort.NEW -> setImageResource(R.drawable.ic_new)
                RedditApi.Sort.TOP -> setImageResource(R.drawable.ic_top)
                RedditApi.Sort.RELEVANCE -> setImageResource(R.drawable.ic_relevance)
                RedditApi.Sort.COMMENTS -> setImageResource(R.drawable.ic_comments)
                else -> {
                    startAnimation(popOutAnimation)
                    return@with
                }
            }

            startAnimation(popInAnimation)
        }

        with(binding.appBar.sortTimeText) {
            val showOutAnimation = isVisible

            visibility = if (!binding.appBar.searchInput.isVisible && (
                sorting.generalSorting == RedditApi.Sort.TOP ||
                    sorting.generalSorting == RedditApi.Sort.RELEVANCE ||
                    sorting.generalSorting == RedditApi.Sort.COMMENTS
                )
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

    private fun showSearchInput(show: Boolean) {
        with(binding.appBar) {
            val searchInputTransition = TransitionSet().apply {
                addTransition(Fade(Fade.OUT))
                addTransition(Slide(Gravity.END))
                addTransition(Fade(Fade.IN))
                duration = 250
                addTarget(searchInput)
            }

            val appBarTransition = MaterialFadeThrough().apply {
                duration = 500
                addTarget(backCard)
                addTarget(label)
                addTarget(sortCard)
                addTarget(sortIcon)
                addTarget(sortTimeText)
                addTarget(searchInput)
            }

            val transitionSet = TransitionSet().apply {
                addTransition(searchInputTransition)
                addTransition(appBarTransition)
            }

            TransitionManager.beginDelayedTransition(root, transitionSet)
            backCard.visibility = if (show) View.GONE else View.VISIBLE
            label.visibility = if (show) View.GONE else View.VISIBLE
            sortCard.visibility = if (show) View.GONE else View.VISIBLE
            sortIcon.visibility = if (show) View.GONE else View.VISIBLE
            sortTimeText.visibility = if (show || viewModel.sorting.value.timeSorting == null) {
                View.GONE
            } else {
                View.VISIBLE
            }
            searchInput.visibility = if (show) View.VISIBLE else View.GONE

            if (show) {
                searchInput.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    showSoftKeyboard()
                }
            } else {
                searchInput.apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    hideSoftKeyboard()
                }
            }
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
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = RedditUri.getSubredditUri(subreddit)
        startActivity(intent)
    }

    private fun onUserClick(user: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = RedditUri.getUserUri(user)
        startActivity(intent)
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
