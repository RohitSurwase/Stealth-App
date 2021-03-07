package com.cosmos.unreddit.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentUserBinding
import com.cosmos.unreddit.databinding.ItemListContentBinding
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.postlist.PostListAdapter
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.getItemView
import com.cosmos.unreddit.util.getRecyclerView
import com.cosmos.unreddit.util.onRefreshFromNetwork
import com.cosmos.unreddit.util.setSortingListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UserFragment : BaseFragment(), PostListAdapter.PostClickListener {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    private val args: UserFragmentArgs by navArgs()

    private var userPostJob: Job? = null
    private var userCommentJob: Job? = null

    private lateinit var postListAdapter: PostListAdapter
    private lateinit var commentListAdapter: UserCommentsAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setUser(args.user)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initResultListener()
        initAppBar()
        initViewPager()
        bindViewModel()
    }

    private fun bindViewModel() {
        viewModel.about.observe(viewLifecycleOwner, this::bindInfo)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.user,
                viewModel.sorting,
                viewModel.page,
                viewModel.contentPreferences
            ) { user, sorting, page, contentPreferences ->
                postListAdapter.setContentPreferences(contentPreferences)
                user?.let {
                    load(page, user, sorting)
                }
                binding.sortIcon.setSorting(sorting)
            }.collect()
        }
    }

    private fun initViewPager() {
        postListAdapter = PostListAdapter(repository, this, this)
        commentListAdapter = UserCommentsAdapter(requireContext(), this)

        val tabs: List<RecyclerViewStateAdapter.Page> = listOf(
            RecyclerViewStateAdapter.Page(R.string.tab_user_submitted, postListAdapter),
            RecyclerViewStateAdapter.Page(R.string.tab_user_comments, commentListAdapter)
        )

        val userStateAdapter = RecyclerViewStateAdapter().apply { submitList(tabs) }
        binding.viewPager.apply {
            adapter = userStateAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
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
                tab?.let { scrollToTop(it.position) }
            }
        })

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.setText(tabs[position].title)
        }.attach()

        lifecycleScope.launch {
            postListAdapter.onRefreshFromNetwork {
                scrollToTop(0)
            }
        }
        lifecycleScope.launch {
            commentListAdapter.onRefreshFromNetwork {
                scrollToTop(1)
            }
        }
    }

    private fun initAppBar() {
        with(binding) {
            sortCard.setOnClickListener { showSortDialog() }
            backCard.setOnClickListener { activity?.onBackPressed() }
        }
    }

    private fun initResultListener() {
        setSortingListener { sorting ->
            sorting?.let { viewModel.setSorting(sorting) }
        }
    }

    private fun bindInfo(user: User) {
        with(user) {
            binding.user = this

            binding.userImage.load(icon) {
                crossfade(true)
                scale(Scale.FILL)
                precision(Precision.AUTOMATIC)
            }
        }
    }

    private fun load(position: Int, user: String, sorting: Sorting) {
        when (position) {
            0 -> loadPosts(user, sorting)
            1 -> loadComments(user, sorting)
        }
    }

    private fun loadPosts(user: String, sorting: Sorting) {
        userPostJob?.cancel()
        userPostJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAndFilterPosts(user, sorting).collectLatest {
                postListAdapter.submitData(it)
            }
        }
    }

    private fun loadComments(user: String, sorting: Sorting) {
        userCommentJob?.cancel()
        userCommentJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAndFilterComments(user, sorting).collectLatest {
                commentListAdapter.submitData(it)
            }
        }
    }

    private fun showSortDialog() {
        SortFragment.show(childFragmentManager, viewModel.sorting.value)
    }

    private fun scrollToTop(page: Int) {
        val itemView = binding.viewPager.getItemView(page)
        itemView?.let {
            ItemListContentBinding.bind(it).apply {
                listContent.betterSmoothScrollToPosition(0)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
