package com.cosmos.unreddit.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.FragmentUserBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.commentmenu.CommentMenuFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.postlist.PostListAdapter
import com.cosmos.unreddit.ui.postmenu.PostMenuFragment
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.RecyclerViewStateAdapter
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import com.cosmos.unreddit.util.extension.scrollToTop
import com.cosmos.unreddit.util.extension.setCommentListener
import com.cosmos.unreddit.util.extension.setSortingListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
class UserFragment : BaseFragment(), UserCommentsAdapter.CommentClickListener {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    override val viewModel: UserViewModel by viewModels()

    private val args: UserFragmentArgs by navArgs()

    private var userPostJob: Job? = null
    private var userCommentJob: Job? = null

    private lateinit var postListAdapter: PostListAdapter
    private lateinit var commentListAdapter: UserCommentsAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setUser(args.user.removeSuffix("/"))
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
        binding.infoRetry.setActionClickListener { retry() }
    }

    private fun bindViewModel() {
        viewModel.about.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> bindInfo(it.data)
                is Resource.Error -> handleError(it.code)
                is Resource.Loading -> {
                    // ignore
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                viewModel.user,
                viewModel.sorting,
                viewModel.page,
                viewModel.contentPreferences
            ) { user, sorting, page, contentPreferences ->
                postListAdapter.contentPreferences = contentPreferences
                user?.let {
                    viewModel.loadUserInfo(false)
                    load(page, user, sorting)
                }
                binding.sortIcon.setSorting(sorting)
            }.collect()
        }
    }

    private fun initViewPager() {
        postListAdapter = PostListAdapter(repository, this, this)
        commentListAdapter = UserCommentsAdapter(requireContext(), this, this)

        val tabs: List<RecyclerViewStateAdapter.Page> = listOf(
            RecyclerViewStateAdapter.Page(R.string.tab_user_submitted, postListAdapter),
            RecyclerViewStateAdapter.Page(R.string.tab_user_comments, commentListAdapter)
        )

        val userStateAdapter = RecyclerViewStateAdapter {
            showRetryBar()
        }.apply {
            submitList(tabs)
        }

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
            commentListAdapter.onRefreshFromNetwork {
                binding.viewPager.scrollToTop(1)
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
        setCommentListener { comment -> comment?.let { viewModel.toggleSaveComment(it) } }
    }

    private fun bindInfo(user: User) {
        if (user.isSuspended) {
            showUnauthorizedDialog()
            return
        }

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

    private fun handleError(code: Int?) {
        when (code) {
            403 -> showUnauthorizedDialog()
            404 -> showNotFoundDialog()
            else -> showRetryBar()
        }
    }

    private fun retry() {
        viewModel.about.value?.let {
            if (it is Resource.Error) {
                viewModel.loadUserInfo(true)
            }
        }
        // TODO: Don't retry if not necessary
        postListAdapter.retry()
        commentListAdapter.retry()
    }

    private fun showRetryBar() {
        if (!binding.infoRetry.isVisible) {
            binding.infoRetry.show()
        }
    }

    private fun showSortDialog() {
        SortFragment.show(childFragmentManager, viewModel.sorting.value)
    }

    private fun showNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_user_not_found_title)
            .setMessage(R.string.dialog_user_not_found_body)
            .setPositiveButton(R.string.dialog_ok) { _, _ -> onBackPressed() }
            .setCancelable(false)
            .show()
    }

    private fun showUnauthorizedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_user_unauthorized_title)
            .setMessage(R.string.dialog_user_unauthorized_body)
            .setPositiveButton(R.string.dialog_ok) { _, _ -> onBackPressed() }
            .setCancelable(false)
            .show()
    }

    override fun onLongClick(post: PostEntity) {
        PostMenuFragment.show(parentFragmentManager, post, PostMenuFragment.MenuType.USER)
    }

    override fun onMenuClick(post: PostEntity) {
        PostMenuFragment.show(parentFragmentManager, post, PostMenuFragment.MenuType.USER)
    }

    override fun onClick(comment: CommentEntity) {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(
                R.id.fragment_container,
                PostDetailsFragment.newInstance(comment.permalink),
                PostDetailsFragment.TAG
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onLongClick(comment: CommentEntity) {
        CommentMenuFragment.show(childFragmentManager, comment, CommentMenuFragment.MenuType.USER)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
