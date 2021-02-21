package com.cosmos.unreddit.subreddit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentSubredditSearchBinding
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListAdapter
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.SearchUtil
import com.cosmos.unreddit.util.hideSoftKeyboard
import com.cosmos.unreddit.util.loadSubredditIcon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SubredditSearchFragment : BaseFragment(), PostListAdapter.PostClickListener,
    ClickableMovementMethod.OnLinkClickListener {

    private var _binding: FragmentSubredditSearchBinding? = null
    private val binding get() = _binding!!

    private val subredditViewModel: SubredditViewModel by activityViewModels()
    private val viewModel: SubredditSearchViewModel by viewModels()

    private var searchPostJob: Job? = null

    private lateinit var adapter: PostListAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val subreddit = arguments?.getString(KEY_SUBREDDIT)
        subreddit?.let {
            viewModel.setSubreddit(it)
        }
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

        // TODO: Animation
        showSearchInput(true)
    }

    private fun bindViewModel() {
        subredditViewModel.about.observe(
            viewLifecycleOwner,
            { subreddit ->
                binding.appBar.subredditImage.loadSubredditIcon(subreddit.icon)
            }
        )
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
                viewModel.sorting
            ) { subreddit, query, sorting ->
                if (subreddit != null && query != null) {
                    searchPost(query, sorting)
                }
                binding.appBar.sortIcon.setSorting(sorting)
            }.collect { scrollToTop() }
        }
    }

    private fun initRecyclerView() {
        adapter = PostListAdapter(repository, this, this).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        with(binding.listPost) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubredditSearchFragment.adapter
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { scrollToTop() }
        }
    }

    private fun initAppBar() {
        with(binding.appBar) {
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
                setOnEditorActionListener { _, actionId, _ ->
                    when (actionId) {
                        EditorInfo.IME_ACTION_SEARCH -> {
                            if (SearchUtil.isQueryValid(text.toString())) {
                                viewModel.setQuery(text.toString())
                                showSearchInput(false)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun initResultListener() {
        childFragmentManager.setFragmentResultListener(
            SortFragment.REQUEST_KEY_SORTING,
            viewLifecycleOwner
        ) { _, bundle ->
            val sorting = bundle.getParcelable(SortFragment.BUNDLE_KEY_SORTING) as? Sorting
            sorting?.let { viewModel.setSorting(it) }
        }
    }

    private fun scrollToTop() {
        binding.listPost.scrollToPosition(0)
    }

    private fun searchPost(query: String, sorting: Sorting) {
        searchPostJob?.cancel()
        searchPostJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchAndFilterPosts(query, sorting).collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun showSearchInput(show: Boolean) {
        binding.appBar.searchInput.show(binding.appBar.root, show) {
            with(binding.appBar) {
                backCard.visibility = if (show) View.GONE else View.VISIBLE
                label.visibility = if (show) View.GONE else View.VISIBLE
                sortCard.visibility = if (show) View.GONE else View.VISIBLE
                sortIcon.visibility = if (show) View.GONE else View.VISIBLE
                subredditImage.visibility = if (show) View.GONE else View.VISIBLE
                cancelCard.visibility = if (show) View.VISIBLE else View.GONE
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

    private fun cancelSearch() {
        if (viewModel.query.value != null) {
            showSearchInput(false)
        } else {
            binding.appBar.searchInput.hideSoftKeyboard()
            activity?.onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (binding.appBar.searchInput.isVisible && viewModel.query.value != null) {
            showSearchInput(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(post: PostEntity) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(post: PostEntity) {
        TODO("Not yet implemented")
    }

    override fun onImageClick(post: PostEntity) {
        TODO("Not yet implemented")
    }

    override fun onVideoClick(post: PostEntity) {
        TODO("Not yet implemented")
    }

    override fun onLinkClick(post: PostEntity) {
        TODO("Not yet implemented")
    }

    override fun onLinkClick(link: String) {
        TODO("Not yet implemented")
    }

    override fun onLinkLongClick(link: String) {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SubredditSearchFragment"

        private const val KEY_SUBREDDIT = "KEY_SUBREDDIT"

        fun newInstance(subreddit: String) = SubredditSearchFragment().apply {
            arguments = bundleOf(
                KEY_SUBREDDIT to subreddit
            )
        }
    }
}
