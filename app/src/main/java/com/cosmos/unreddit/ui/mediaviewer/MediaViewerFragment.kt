package com.cosmos.unreddit.ui.mediaviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.databinding.FragmentMediaViewerBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaViewerFragment : BaseFragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaViewerViewModel by viewModels()

    private val args: MediaViewerFragmentArgs by navArgs()

    // Flag to check if fragment was open from FragmentManager or Navigation
    private var isLegacyNavigation: Boolean = false

    private lateinit var mediaAdapter: MediaViewerAdapter
    private lateinit var thumbnailAdapter: MediaViewerThumbnailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initViewPager()
        bindViewModel()
        binding.infoRetry.setActionClickListener { retry() }
    }

    private fun bindViewModel() {
        viewModel.media.observe(viewLifecycleOwner) {
            binding.loadingCradle.isVisible = it is Resource.Loading
            when (it) {
                is Resource.Success -> bindMedia(it.data)
                is Resource.Error -> handleError(it.code)
                is Resource.Loading -> {
                    // ignore
                }
            }
        }
        viewModel.selectedPage.observe(viewLifecycleOwner) {
            binding.listThumbnails.betterSmoothScrollToPosition(it)
            thumbnailAdapter.selectItem(it)
            binding.textPageCurrent.text = it.plus(1).toString()
        }
    }

    private fun initRecyclerView() {
        thumbnailAdapter = MediaViewerThumbnailAdapter(this::onThumbnailClick)
        binding.listThumbnails.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = thumbnailAdapter
        }
    }

    private fun initViewPager() {
        mediaAdapter = MediaViewerAdapter(requireContext())
        binding.viewPager.apply {
            adapter = mediaAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.setSelectedPage(position)
                }
            })
        }
    }

    private fun onThumbnailClick(position: Int) {
        binding.viewPager.currentItem = position
    }

    private fun handleArguments() {
        if (args.images != null) {
            viewModel.setMedia(args.images!!.toList())
        } else if (args.link != null && args.type != MediaType.NO_MEDIA) {
            viewModel.loadMedia(args.link!!, args.type)
        } else {
            arguments?.let { bundle ->
                if (bundle.containsKey(BUNDLE_KEY_IMAGES)) {
                    val images = bundle.getParcelableArrayList<GalleryMedia>(BUNDLE_KEY_IMAGES)
                    if (images != null) {
                        viewModel.setMedia(images.toList())
                    }
                } else if (bundle.containsKey(BUNDLE_KEY_LINK)) {
                    val link = bundle.getString(BUNDLE_KEY_LINK, "")
                    val type = bundle.getSerializable(BUNDLE_KEY_TYPE) as? MediaType
                        ?: MediaType.LINK
                    viewModel.loadMedia(link, type)
                }
                isLegacyNavigation = true
            }
        }
    }

    private fun bindMedia(media: List<GalleryMedia>) {
        if (media.size > 1) {
            thumbnailAdapter.submitData(media)
            binding.textPageCount.text = media.size.toString()
            binding.listThumbnails.visibility = View.VISIBLE
            binding.flowPageCounter.visibility = View.VISIBLE
        } else {
            binding.listThumbnails.visibility = View.GONE
            binding.flowPageCounter.visibility = View.GONE
        }
        mediaAdapter.submitData(media)
    }

    private fun handleError(code: Int?) {
        when (code) {
            403, 404 -> showNotFoundDialog()
            else -> showRetryBar()
        }
    }

    private fun retry() {
        if (args.link != null) {
            viewModel.loadMedia(args.link!!, args.type, true)
        } else {
            val link = arguments?.getString(BUNDLE_KEY_LINK)
            val type = arguments?.getSerializable(BUNDLE_KEY_TYPE) as? MediaType
            if (link != null && type != null) {
                viewModel.loadMedia(link, type, true)
            }
        }
    }

    private fun showRetryBar() {
        if (!binding.infoRetry.isVisible) {
            binding.infoRetry.show()
        }
    }

    private fun showNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_media_not_found_title)
            .setMessage(R.string.dialog_media_not_found_body)
            .setPositiveButton(R.string.dialog_ok) { _, _ -> onBackPressed() }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        if (isLegacyNavigation) {
            // Prevent onBackPressed event to be passed to PostDetailsFragment and show bottom nav
            parentFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaAdapter.clear()
        _binding = null
    }

    companion object {
        const val TAG = "ImageViewerFragment"

        private const val BUNDLE_KEY_IMAGES = "BUNDLE_KEY_IMAGES"
        private const val BUNDLE_KEY_LINK = "BUNDLE_KEY_LINK"
        private const val BUNDLE_KEY_TYPE = "BUNDLE_KEY_TYPE"

        fun newInstance(images: List<GalleryMedia>) = MediaViewerFragment().apply {
            arguments = bundleOf(
                BUNDLE_KEY_IMAGES to images
            )
        }

        fun newInstance(link: String, type: MediaType) = MediaViewerFragment().apply {
            arguments = bundleOf(
                BUNDLE_KEY_LINK to link,
                BUNDLE_KEY_TYPE to type
            )
        }
    }
}
