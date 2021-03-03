package com.cosmos.unreddit.subreddit

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentSubredditBinding
import com.cosmos.unreddit.databinding.LayoutSubredditAboutBinding
import com.cosmos.unreddit.databinding.LayoutSubredditContentBinding
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListAdapter
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.toPixels
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
class SubredditFragment : BaseFragment(), PostListAdapter.PostClickListener, View.OnClickListener,
    ClickableMovementMethod.OnLinkClickListener {

    private var _binding: FragmentSubredditBinding? = null
    private val binding get() = _binding!!

    private var _bindingContent: LayoutSubredditContentBinding? = null
    private val bindingContent get() = _bindingContent!!

    private var _bindingAbout: LayoutSubredditAboutBinding? = null
    private val bindingAbout get() = _bindingAbout!!

    private val viewModel: SubredditViewModel by activityViewModels()

    private var loadPostsJob: Job? = null

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
        _binding = FragmentSubredditBinding.inflate(inflater, container, false)
        _bindingContent = binding.subredditContent
        _bindingAbout = binding.subredditAbout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initResultListener()
        initAppBar()
        initRecyclerView()
        initDrawer()
        bindViewModel()
        bindingAbout.subredditSubscribeButton.setOnClickListener(this)
    }

    private fun bindViewModel() {
        viewModel.about.observe(viewLifecycleOwner, this::bindInfo)
        viewModel.isSubscribed.observe(
            viewLifecycleOwner,
            { isSubscribed ->
                with(bindingAbout.subredditSubscribeButton) {
                    visibility = View.VISIBLE
                    text = if (isSubscribed) {
                        getString(R.string.subreddit_button_unsubscribe)
                    } else {
                        getString(R.string.subreddit_button_subscribe)
                    }
                }
            }
        )
        viewModel.isDescriptionCollapsed.observe(
            viewLifecycleOwner,
            { isCollapsed ->
                // TODO: Animate layout changes
                val maxHeight = if (isCollapsed) {
                    requireContext().toPixels(DESCRIPTION_MAX_HEIGHT).toInt()
                } else {
                    Integer.MAX_VALUE
                }
                ConstraintSet().apply {
                    clone(bindingAbout.layoutRoot)
                    constrainMaxHeight(R.id.subreddit_public_description, maxHeight)
                    applyTo(bindingAbout.layoutRoot)
                }
            }
        )
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.subreddit,
                viewModel.sorting,
                viewModel.contentPreferences
            ) { subreddit, sorting, contentPreferences ->
                adapter.setContentPreferences(contentPreferences)
                subreddit?.let {
                    loadPosts(subreddit, sorting)
                }
                bindingContent.sortIcon.setSorting(sorting)
            }.collect { scrollToTop() }
        }
    }

    private fun initRecyclerView() {
        adapter = PostListAdapter(repository, this, clickableMovementMethod).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        with(bindingContent.listPost) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubredditFragment.adapter
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { scrollToTop() }
        }
    }

    private fun initDrawer() {
        with(binding.drawerLayout) {
            setScrimColor(Color.TRANSPARENT)
            drawerElevation = 0F
        }
        bindingAbout.subredditPublicDescription.setOnClickListener {
            viewModel.toggleDescriptionCollapsed()
        }
    }

    private fun initAppBar() {
        with(bindingContent) {
            sortCard.setOnClickListener { showSortDialog() }
            backCard.setOnClickListener { activity?.onBackPressed() }
            searchCard.setOnClickListener { showSearchFragment() }
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

    private fun bindInfo(about: SubredditEntity) {
        with(about) {
            bindingContent.subreddit = this
            bindingAbout.subreddit = this

            bindingContent.subredditImage.load(icon) {
                crossfade(true)
                scale(Scale.FILL)
                precision(Precision.AUTOMATIC)
                placeholder(R.drawable.icon_reddit_placeholder)
                error(R.drawable.icon_reddit_placeholder)
                fallback(R.drawable.icon_reddit_placeholder)
            }

            if (publicDescription.isNotEmpty()) {
                bindingAbout.subredditPublicDescription.setText(
                    publicDescription,
                    clickableMovementMethod
                )
            } else {
                bindingAbout.subredditPublicDescription.visibility = View.GONE
            }
            if (description.isNotEmpty()) {
                bindingAbout.subredditDescription.setText(description, clickableMovementMethod)
            }
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

    private fun scrollToTop() {
        // TODO: Find better method when item is too far
        bindingContent.listPost.scrollToPosition(0)
    }

    private fun showSearchFragment() {
        // TODO: Navigation
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            add(
                R.id.fragment_container,
                SubredditSearchFragment.newInstance(viewModel.subreddit.value!!),
                SubredditSearchFragment.TAG
            )
            addToBackStack(null)
        }
    }

    private fun showSortDialog() {
        SortFragment.show(childFragmentManager, viewModel.sorting.value)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _bindingContent = null
        _bindingAbout = null
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            bindingAbout.subredditSubscribeButton.id -> {
                viewModel.toggleSubscription()
            }
        }
    }

    companion object {
        private const val KEY_SUBREDDIT = "KEY_SUBREDDIT"

        private const val DESCRIPTION_MAX_HEIGHT = 200F

        @JvmStatic
        fun newInstance(subreddit: String) = SubredditFragment().apply {
            arguments = bundleOf(
                KEY_SUBREDDIT to subreddit
            )
        }

        fun newInstance() = SubredditFragment()
    }
}
