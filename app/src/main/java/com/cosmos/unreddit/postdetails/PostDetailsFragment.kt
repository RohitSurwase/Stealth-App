package com.cosmos.unreddit.postdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.R
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentPostDetailsBinding
import com.cosmos.unreddit.mediaviewer.MediaViewerFragment
import com.cosmos.unreddit.model.GalleryMedia
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.sort.SortFragment
import com.cosmos.unreddit.util.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.setSortingListener
import com.cosmos.unreddit.view.ElasticDragDismissFrameLayout
import dagger.hilt.android.AndroidEntryPoint
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

    private lateinit var postAdapter: PostAdapter
    private lateinit var commentAdapter: CommentAdapter

    @Inject
    lateinit var repository: PostListRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args.subreddit != null && args.id != null) {
            val permalink = "/r/${args.subreddit}/comments/${args.id}"
            viewModel.setPermalink(permalink)
        } else {
            val post = arguments?.getParcelable(KEY_POST_ENTITY) as? PostEntity
            post?.let {
                viewModel.setSorting(it.suggestedSorting)
                viewModel.setPermalink(it.permalink)
            }
        }
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
    }

    private fun initRecyclerView() {
        postAdapter = PostAdapter(this, this)
        commentAdapter = CommentAdapter(
            requireContext(),
            repository,
            viewLifecycleOwner,
            this
        )
        val concatAdapter = ConcatAdapter(postAdapter, commentAdapter)
        binding.listComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
        }
    }

    private fun bindViewModel() {
        viewModel.post.observe(viewLifecycleOwner, { bindPost(it, false) })
        viewModel.comments.observe(
            viewLifecycleOwner,
            { comments ->
                commentAdapter.submitData(comments)
            }
        )
        viewModel.sorting.asLiveData().observe(
            viewLifecycleOwner,
            {
                binding.appBar.sortIcon.setSorting(it)
            }
        )
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
        commentAdapter.setLinkId(post.id)
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
        parentFragmentManager.popBackStack()
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
        showNavigation(true)
        parentFragmentManager.popBackStack()
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

        @JvmStatic
        fun newInstance(post: PostEntity) = PostDetailsFragment().apply {
            arguments = bundleOf(
                KEY_POST_ENTITY to post
            )
        }
    }
}
