package com.cosmos.unreddit.ui.mediaviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.data.worker.MediaDownloadWorker
import com.cosmos.unreddit.databinding.FragmentMediaViewerBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.getRecyclerView
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.showWindowInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MediaViewerFragment : BaseFragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!

    private val viewerViewModel: MediaViewerViewModel by viewModels()

    private val args: MediaViewerFragmentArgs by navArgs()

    // Flag to check if fragment was open from FragmentManager or Navigation
    private var isLegacyNavigation: Boolean = false

    private lateinit var mediaAdapter: MediaViewerAdapter
    private lateinit var thumbnailAdapter: MediaViewerThumbnailAdapter

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadMedia()
        } else {
            Snackbar.make(
                binding.root,
                R.string.snackbar_permission_storage_denied_message,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { preferencesRepository.getMuteVideo(false).first() }
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

        showSystemBars(false)

        initRecyclerView()
        initViewPager()
        bindViewModel()
        binding.infoRetry.setActionClickListener { retry() }
    }

    override fun applyInsets(view: View) {
        // Don't apply any insets
    }

    private fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewerViewModel.media.collect {
                    binding.loadingCradle.isVisible = it is Resource.Loading
                    when (it) {
                        is Resource.Success -> bindMedia(it.data)
                        is Resource.Error -> handleError(it.code)
                        is Resource.Loading -> {
                            // ignore
                        }
                    }
                }
            }

            launch {
                viewerViewModel.selectedPage.collect {
                    binding.listThumbnails.betterSmoothScrollToPosition(it)
                    thumbnailAdapter.selectItem(it)
                    binding.textPageCurrent.text = it.plus(1).toString()
                }
            }
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
        val muteVideo = runBlocking { preferencesRepository.getMuteVideo(false).first() }

        mediaAdapter = MediaViewerAdapter(
            requireContext(),
            muteVideo,
            onMuteClick = {
                lifecycleScope.launch { preferencesRepository.setMuteVideo(it) }
            },
            onDownloadClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // No need to request storage permission on Android 10+
                    downloadMedia()
                } else {
                    requestMediaDownload()
                }
            }
        )

        binding.viewPager.apply {
            adapter = mediaAdapter
            getRecyclerView()?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewerViewModel.setSelectedPage(position)
                }
            })
        }
    }

    private fun onThumbnailClick(position: Int) {
        binding.viewPager.currentItem = position
    }

    private fun handleArguments() {
        if (args.images != null) {
            viewerViewModel.setMedia(args.images!!.toList())
        } else if (args.link != null && args.type != MediaType.NO_MEDIA) {
            viewerViewModel.loadMedia(args.link!!, args.type)
        } else {
            arguments?.let { bundle ->
                if (bundle.containsKey(BUNDLE_KEY_IMAGES)) {
                    val images = bundle.getParcelableArrayList<GalleryMedia>(BUNDLE_KEY_IMAGES)
                    if (images != null) {
                        viewerViewModel.setMedia(images.toList())
                    }
                } else if (bundle.containsKey(BUNDLE_KEY_LINK)) {
                    val link = bundle.getString(BUNDLE_KEY_LINK, "")
                    val type = bundle.getSerializable(BUNDLE_KEY_TYPE) as? MediaType
                        ?: MediaType.LINK
                    viewerViewModel.loadMedia(link, type)
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

    private fun requestMediaDownload() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                downloadMedia()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Snackbar.make(
                    binding.root,
                    R.string.snackbar_permission_storage_request_message,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadMedia() {
        val page = viewerViewModel.selectedPage.value

        val media = mediaAdapter.getItem(page)
        media?.let {
            MediaDownloadWorker.enqueueWork(
                requireContext().applicationContext,
                it.url,
                it.type
            )
        }
    }

    private fun handleError(code: Int?) {
        when (code) {
            403, 404 -> showNotFoundDialog()
            else -> showRetryBar()
        }
    }

    private fun retry() {
        if (args.link != null) {
            viewerViewModel.loadMedia(args.link!!, args.type, true)
        } else {
            val link = arguments?.getString(BUNDLE_KEY_LINK)
            val type = arguments?.getSerializable(BUNDLE_KEY_TYPE) as? MediaType
            if (link != null && type != null) {
                viewerViewModel.loadMedia(link, type, true)
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

    private fun showSystemBars(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.decorView?.showWindowInsets(show)
        } else {
            binding.root.showWindowInsets(show)
        }
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
        showSystemBars(true)
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
