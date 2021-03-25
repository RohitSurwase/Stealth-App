package com.cosmos.unreddit.ui.postdetails

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.databinding.FragmentPostDetailsBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.ui.common.ElasticDragDismissFrameLayout
import com.cosmos.unreddit.ui.loadstate.ResourceStateAdapter
import com.cosmos.unreddit.ui.mediaviewer.MediaViewerFragment
import com.cosmos.unreddit.ui.sort.SortFragment
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.setSortingListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PostDetailsFragment :
    BaseFragment(),
    ElasticDragDismissFrameLayout.ElasticDragDismissCallback {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailsViewModel by viewModels()

    private val args: PostDetailsFragmentArgs by navArgs()

    private val contentRadius by lazy { resources.getDimension(R.dimen.subreddit_content_radius) }
    private val contentElevation by lazy {
        resources.getDimension(R.dimen.subreddit_content_elevation)
    }

    private var isLegacyNavigation: Boolean = false

    private lateinit var postAdapter: PostAdapter
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var resourceStateAdapter: ResourceStateAdapter

    @Inject
    lateinit var repository: PostListRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenStarted {
            preferencesRepository.getContentPreferences().first()
        }

        if (savedInstanceState == null) {
            handleArguments()
        }

        isLegacyNavigation = (args.subreddit == null || args.id == null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showNavigation(false)

        binding.root.addListener(this)

        initResultListener()
        initAppBar()
        initRecyclerView()

        val post = arguments?.getParcelable(KEY_POST_ENTITY) as? PostEntity
        post?.let {
            bindPost(it, true)
        }

        bindViewModel()

        binding.singleThreadLayout.setOnClickListener { loadFullDiscussion() }
    }

    private fun initRecyclerView() {
        val contentPreferences = runBlocking {
            preferencesRepository.getContentPreferences().first()
        }

        postAdapter = PostAdapter(contentPreferences, this, this)
        commentAdapter = CommentAdapter(requireContext(), repository, viewLifecycleOwner, this)
        resourceStateAdapter = ResourceStateAdapter { retry() }

        val concatAdapter = ConcatAdapter(postAdapter, resourceStateAdapter, commentAdapter)
        binding.listComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
        }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(viewModel.permalink, viewModel.sorting) { permalink, _ ->
                permalink?.let {
                    viewModel.loadPost(false)
                }
            }.collect()
        }
        viewModel.post.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> bindPost(it.data, false)
                else -> {
                    // ignore
                }
            }
        }
        viewModel.comments.observe(viewLifecycleOwner) {
            resourceStateAdapter.resource = it
            when (it) {
                is Resource.Success -> commentAdapter.submitData(it.data)
                else -> {
                    // ignore
                }
            }
        }
        viewModel.sorting.asLiveData().observe(
            viewLifecycleOwner,
            {
                binding.appBar.sortIcon.setSorting(it)
            }
        )
        viewModel.singleThread.observe(viewLifecycleOwner) { isSingleThread ->
            val transition = Slide(Gravity.TOP).apply {
                duration = 500
                addTarget(binding.singleThreadLayout)
            }
            TransitionManager.beginDelayedTransition(binding.root, transition)
            binding.singleThreadLayout.visibility = if (isSingleThread) View.VISIBLE else View.GONE
        }
    }

    private fun initAppBar() {
        with(binding.appBar) {
            backCard.setOnClickListener { onBackPressed() }
            sortCard.setOnClickListener { showSortDialog() }
        }
    }

    private fun initResultListener() {
        setSortingListener { sorting ->
            sorting?.let {
                viewModel.setSorting(sorting)
                binding.listComments.betterSmoothScrollToPosition(0)
            }
        }
    }

    private fun bindPost(post: PostEntity, fromCache: Boolean) {
        binding.appBar.label.text = post.title
        postAdapter.setPost(post, fromCache)
        commentAdapter.linkId = post.id
    }

    private fun handleArguments() {
        if (args.subreddit != null && args.id != null) {
            val stringBuilder = StringBuilder().apply {
                append("/r/").append(args.subreddit).append("/comments/").append(args.id)
            }

            if (args.title != null && args.comment != null) {
                stringBuilder.append("/").append(args.title).append("/").append(args.comment)
                viewModel.setSingleThread(true)
            }

            val permalink = stringBuilder.toString()

            viewModel.setPermalink(permalink)
        } else {
            if (arguments?.containsKey(KEY_POST_ENTITY) == true) {
                val post = arguments?.getParcelable(KEY_POST_ENTITY) as? PostEntity
                post?.let {
                    viewModel.setPermalink(it.permalink)
                }
            } else if (arguments?.containsKey(KEY_THREAD_PERMALINK) == true) {
                val threadPermalink = arguments?.getString(KEY_THREAD_PERMALINK)
                threadPermalink?.let {
                    viewModel.setPermalink(it)
                    viewModel.setSingleThread(true)
                }
            }
        }
    }

    private fun loadFullDiscussion() {
        val permalink = viewModel.permalink.value
        permalink?.let {
            val newPermalink = it.removeSuffix("/").substringBeforeLast("/")
            viewModel.setPermalink(newPermalink)
            viewModel.setSingleThread(false)
            viewModel.loadPost(false)
        }
    }

    private fun retry() {
        viewModel.loadPost(true)
    }

    private fun showSortDialog() {
        SortFragment.show(
            childFragmentManager,
            viewModel.sorting.value,
            SortFragment.SortType.POST
        )
    }

    private fun showMediaViewer(mediaViewerFragment: MediaViewerFragment) {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.fragment_container, mediaViewerFragment, MediaViewerFragment.TAG)
            .addToBackStack(null)
            .commit()
    }

    private fun showNavigation(show: Boolean) {
        setFragmentResult(REQUEST_KEY_NAVIGATION, bundleOf(BUNDLE_KEY_NAVIGATION to show))
    }

    override fun onBackPressed() {
        showNavigation(true)
        if (isLegacyNavigation) {
            // Prevent onBackPressed event to be passed to PostDetailsFragment and show bottom nav
            parentFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDrag(
        elasticOffset: Float,
        elasticOffsetPixels: Float,
        rawOffset: Float,
        rawOffsetPixels: Float
    ) {
        binding.root.cardElevation = contentElevation * rawOffset
        binding.root.radius = contentRadius * rawOffset
    }

    override fun onDragDismissed() {
        onBackPressed()
    }

    override fun openGallery(images: List<GalleryMedia>) {
        showMediaViewer(MediaViewerFragment.newInstance(images))
    }

    override fun openMedia(link: String, mediaType: MediaType) {
        showMediaViewer(MediaViewerFragment.newInstance(link, mediaType))
    }

    companion object {
        const val TAG = "PostDetailsFragment"

        const val REQUEST_KEY_NAVIGATION = "REQUEST_KEY_NAVIGATION"
        const val BUNDLE_KEY_NAVIGATION = "BUNDLE_KEY_NAVIGATION"

        private const val KEY_POST_ENTITY = "KEY_POST_ENTITY"

        private const val KEY_THREAD_PERMALINK = "KEY_THREAD_PERMALINK"

        @JvmStatic
        fun newInstance(post: PostEntity) = PostDetailsFragment().apply {
            arguments = bundleOf(
                KEY_POST_ENTITY to post
            )
        }

        fun newInstance(threadPermalink: String) = PostDetailsFragment().apply {
            arguments = bundleOf(
                KEY_THREAD_PERMALINK to threadPermalink
            )
        }
    }
}
