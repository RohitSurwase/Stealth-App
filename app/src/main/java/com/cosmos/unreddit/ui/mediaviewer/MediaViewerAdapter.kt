package com.cosmos.unreddit.ui.mediaviewer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource

class MediaViewerAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            binding.image.setOnTouchListener(this)
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
                                binding.loadingCradle.isVisible = true
                                binding.infoRetry.hide()
                            },
                            onCancel = {
                                binding.loadingCradle.isVisible = false
                            },
                            onError = { _, _ ->
                                binding.loadingCradle.isVisible = false
                                binding.infoRetry.show()
                            },
                            onSuccess = { _, _ ->
                                binding.loadingCradle.isVisible = false
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
    ) : RecyclerView.ViewHolder(binding.root), Player.EventListener {

        fun bind(video: GalleryMedia) {
            val videoItem = exoPlayerHelper.getMediaItem(video.url)

            val player = SimpleExoPlayer.Builder(binding.video.context)
                .setMediaSourceFactory(exoPlayerHelper.defaultMediaSourceFactory)
                .build()

            if (video.sound != null) {
                val videoSource = exoPlayerHelper.getMediaSource(videoItem)
                val audioSource = exoPlayerHelper.getMediaSource(video.sound)
                val mergedSource = MergingMediaSource(videoSource, audioSource)
                player.setMediaSource(mergedSource)
            } else {
                player.setMediaItem(videoItem)
            }

            player.apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(this@VideoViewHolder)
                prepare()
                play()
            }

            players.add(player)

            binding.video.player = player

            binding.infoRetry.setActionClickListener { player.prepare() }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            binding.infoRetry.show()
        }
    }
}
