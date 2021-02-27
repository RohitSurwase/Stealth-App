package com.cosmos.unreddit.mediaviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentMediaViewerBinding
import com.cosmos.unreddit.model.GalleryMedia
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.util.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.getRecyclerView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaViewerFragment : BaseFragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaViewerViewModel by viewModels()
    private val uiViewModel: UiViewModel by activityViewModels()

    private lateinit var mediaAdapter: MediaViewerAdapter
    private lateinit var thumbnailAdapter: MediaViewerThumbnailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            if (bundle.containsKey(BUNDLE_KEY_IMAGES)) {
                val images = bundle.getParcelableArrayList<GalleryMedia>(BUNDLE_KEY_IMAGES)
                if (images != null) {
                    viewModel.setMedia(images.toList())
                }
            } else if (bundle.containsKey(BUNDLE_KEY_LINK)) {
                val link = bundle.getString(BUNDLE_KEY_LINK, "")
                val type = bundle.getSerializable(BUNDLE_KEY_TYPE) as? MediaType ?: MediaType.LINK
                viewModel.loadMedia(link, type)
            }
        }
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
        uiViewModel.setNavigationVisibility(false)
        initRecyclerView()
        initViewPager()
        bindViewModel()
    }

    private fun bindViewModel() {
        viewModel.media.observe(
            viewLifecycleOwner,
            { media ->
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
        )
        viewModel.selectedPage.observe(
            viewLifecycleOwner,
            {
                binding.listThumbnails.betterSmoothScrollToPosition(it)
                thumbnailAdapter.selectItem(it)
                binding.textPageCurrent.text = it.plus(1).toString()
            }
        )
    }

    private fun initRecyclerView() {
        thumbnailAdapter = MediaViewerThumbnailAdapter(this::onThumbnailClick)
        binding.listThumbnails.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = thumbnailAdapter
        }
    }

    private fun initViewPager() {
        mediaAdapter = MediaViewerAdapter()
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

    override fun onBackPressed() {
        uiViewModel.setNavigationVisibility(true)
        super.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
