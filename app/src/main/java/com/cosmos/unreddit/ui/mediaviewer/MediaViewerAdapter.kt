package com.cosmos.unreddit.ui.mediaviewer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.databinding.ItemImageBinding
import com.cosmos.unreddit.databinding.ItemVideoBinding
import com.cosmos.unreddit.util.ExoPlayerHelper
import com.cosmos.unreddit.util.LinkUtil
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.HttpDataSource
import okhttp3.HttpUrl
import java.net.HttpURLConnection

class MediaViewerAdapter(
    context: Context,
    private val muteVideo: Boolean,
    private val onMuteClick: (Boolean) -> Unit,
    private val onDownloadClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val media: MutableList<GalleryMedia> = mutableListOf()

    private val players: MutableList<Player> = mutableListOf()

    private val exoPlayerHelper by lazy { ExoPlayerHelper(context) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            GalleryMedia.Type.IMAGE.value -> ImageViewHolder(
                ItemImageBinding.inflate(inflater, parent, false)
            )
            GalleryMedia.Type.VIDEO.value -> VideoViewHolder(
                ItemVideoBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            GalleryMedia.Type.IMAGE.value -> (holder as ImageViewHolder).bind(media[position])
            GalleryMedia.Type.VIDEO.value -> (holder as VideoViewHolder).bind(media[position])
        }
    }

    override fun getItemCount(): Int {
        return media.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (media[position].type) {
            GalleryMedia.Type.IMAGE -> GalleryMedia.Type.IMAGE.value
            GalleryMedia.Type.VIDEO -> GalleryMedia.Type.VIDEO.value
        }
    }

    fun getItem(position: Int): GalleryMedia? {
        return media.getOrNull(position)
    }

    fun submitData(images: List<GalleryMedia>) {
        this.media.clear()
        this.media.addAll(images)
        notifyDataSetChanged()
    }

    fun clear() {
        for (player in players) {
            player.release()
        }
        players.clear()
        exoPlayerHelper.clearCache()
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnTouchListener {

        init {
            binding.image.run {
                setOnTouchListener(this@ImageViewHolder)
                setOnClickListener {
                    binding.buttonDownload.isVisible = !binding.buttonDownload.isVisible
                }
            }
            binding.buttonDownload.setOnClickListener { onDownloadClick.invoke() }
        }

        fun bind(image: GalleryMedia) {
            loadImage(image)
            binding.infoRetry.setActionClickListener { loadImage(image) }
        }

        private fun loadImage(image: GalleryMedia) {
            with(binding.image) {
                Coil.imageLoader(context).enqueue(
                    ImageRequest.Builder(context).apply {
                        data(image.url)
                        crossfade(true)
                        scale(Scale.FILL)
                        precision(Precision.EXACT)
                        memoryCachePolicy(CachePolicy.READ_ONLY)
                        diskCachePolicy(CachePolicy.READ_ONLY)
                        listener(
                            onStart = {
                                binding.run {
                                    loadingCradle.isVisible = true
                                    buttonDownload.isVisible = false
                                    infoRetry.hide()
                                }
                            },
                            onCancel = {
                                binding.run {
                                    loadingCradle.isVisible = false
                                    buttonDownload.isVisible = false
                                }
                            },
                            onError = { _, _ ->
                                binding.run {
                                    loadingCradle.isVisible = false
                                    buttonDownload.isVisible = false
                                    infoRetry.show()
                                }
                            },
                            onSuccess = { _, _ ->
                                binding.run {
                                    loadingCradle.isVisible = false
                                    buttonDownload.isVisible = true
                                }
                            }
                        )
                        target { drawable -> setImageDrawable(drawable) }
                    }.build()
                )
            }
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            return if (
                event.pointerCount >= 2 ||
                view.canScrollHorizontally(1) &&
                binding.image.canScrollHorizontally(-1)
            ) {
                // Multi-touch event
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Disallow RecyclerView to intercept touch events.
                        binding.image.parent.requestDisallowInterceptTouchEvent(true)
                        // Disable touch on view
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        // Allow RecyclerView to intercept touch events.
                        binding.image.parent.requestDisallowInterceptTouchEvent(false)
                        true
                    }
                    else -> true
                }
            } else {
                true
            }
        }
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root), Player.Listener {

        init {
            binding.run {
                controls.isVisible = false
                buttonDownload.setOnClickListener { onDownloadClick.invoke() }
            }
        }

        fun bind(video: GalleryMedia) {
            val url = HttpUrl.parse(video.url) ?: return

            if (url.host().contains("redgifs", ignoreCase = true)) {
                val requestProperties = url
                    .queryParameterNames()
                    .associateWith { url.queryParameter(it) ?: "" }

                exoPlayerHelper.setRequestProperties(requestProperties)
            }

            val player = SimpleExoPlayer.Builder(binding.video.context)
                .setMediaSourceFactory(exoPlayerHelper.defaultMediaSourceFactory)
                .build()

            val videoItem = exoPlayerHelper.getMediaItem(video.url)

            if (video.sound != null) {
                val videoSource = exoPlayerHelper.getMediaSource(videoItem)
                val audioSource = exoPlayerHelper.getMediaSource(video.sound)
                val mergedSource = MergingMediaSource(videoSource, audioSource)

                player.setMediaSource(mergedSource)

                // Add special listener for Reddit videos with audio
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        if (isErrorFromAudio(error)) {
                            // Retry without audio if an error is thrown
                            player.setMediaItem(videoItem)
                            player.prepare()
                        } else {
                            binding.infoRetry.show()
                        }
                    }

                    override fun onTracksChanged(
                        trackGroups: TrackGroupArray,
                        trackSelections: TrackSelectionArray
                    ) {
                        initAudioVolume(trackGroups)
                    }
                })
            } else {
                player.setMediaSource(exoPlayerHelper.getMediaSource(videoItem))
                player.addListener(this)
            }

            player.apply {
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
                play()
            }

            players.add(player)

            binding.video.run {
                this.player = player
                setControllerVisibilityListener { controllerVisibility ->
                    binding.controls.visibility = controllerVisibility
                }
            }

            binding.infoRetry.setActionClickListener { player.prepare() }
        }

        private fun isErrorFromAudio(error: PlaybackException): Boolean {
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                val cause = error.cause as? HttpDataSource.InvalidResponseCodeException
                cause?.dataSpec?.key?.let { link ->
                    return (cause.responseCode == HttpURLConnection.HTTP_FORBIDDEN ||
                            cause.responseCode == HttpURLConnection.HTTP_NOT_FOUND) &&
                            LinkUtil.isRedditSoundTrack(link)
                }
            }
            return false
        }

        private fun initAudioVolume(trackGroups: TrackGroupArray) {
            if (hasAudio(trackGroups)) {
                muteAudio(muteVideo)

                binding.mute.isVisible = true

                binding.mute.run {
                    isChecked = muteVideo
                    setOnCheckedChangeListener { _, isMuted ->
                        muteAudio(isMuted)
                        onMuteClick.invoke(isMuted)
                    }
                }
            } else {
                binding.mute.isVisible = false
            }
        }

        private fun muteAudio(shouldMute: Boolean) {
            (binding.video.player as? SimpleExoPlayer)?.volume = if (shouldMute) 0F else 1F
        }

        private fun hasAudio(trackGroups: TrackGroupArray): Boolean {
            if (!trackGroups.isEmpty) {
                for (arrayIndex in 0 until trackGroups.length) {
                    for (groupIndex in 0 until trackGroups[arrayIndex].length) {
                        val sampleMimeType = trackGroups[arrayIndex].getFormat(groupIndex)
                            .sampleMimeType
                        if (sampleMimeType != null && sampleMimeType.contains("audio")) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        override fun onPlayerError(error: PlaybackException) {
            binding.infoRetry.show()
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            initAudioVolume(trackGroups)
        }
    }
}
