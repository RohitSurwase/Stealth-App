package com.cosmos.unreddit.mediaviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentMediaViewerBinding
import com.cosmos.unreddit.model.MediaType
import com.cosmos.unreddit.util.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.getRecyclerView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaViewerFragment : BaseFragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaViewerViewModel by viewModels()

    private val args: MediaViewerFragmentArgs by navArgs()

    private lateinit var mediaAdapter: MediaViewerAdapter
    private lateinit var thumbnailAdapter: MediaViewerThumbnailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args.images != null) {
            viewModel.setMedia(args.images!!.toList())
        } else if (args.link != null && args.type != MediaType.NO_MEDIA) {
            viewModel.loadMedia(args.link!!, args.type)
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

    override fun onDestroyView() {
        super.onDestroyView()
        mediaAdapter.clear()
        _binding = null
    }

    companion object {
        const val TAG = "ImageViewerFragment"
    }
}
