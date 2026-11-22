package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.util.LruCache
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Block.ImageBlock
import com.cosmos.unreddit.data.model.Block.*
import com.cosmos.unreddit.data.model.HtmlBlock
import com.cosmos.unreddit.data.model.RedditText
import com.cosmos.unreddit.util.ClickableMovementMethod
import com.cosmos.unreddit.util.extension.load
import coil.size.Scale
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.video.VideoSize
import kotlin.math.roundToInt

class RedditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr), ClickableMovementMethod.OnClickListener {

    interface OnLinkClickListener {
        fun onLinkClick(link: String)

        fun onLinkLongClick(link: String)
    }

    private val childParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    private val clickableMovementMethod = ClickableMovementMethod(this)

    private var onLinkClickListener: OnLinkClickListener? = null

    init {
        orientation = VERTICAL
    }

    fun setText(redditText: RedditText) {
        removeAllViews()

        val blocks = redditText.blocks
        for (block in blocks) {
            when (block.type) {
                HtmlBlock.BlockType.TEXT -> {
                    addText(block.block as TextBlock)
                }
                HtmlBlock.BlockType.CODE -> {
                    addCode(block.block as TextBlock)
                }
                HtmlBlock.BlockType.TABLE -> {
                    addTable(block.block as TableBlock)
                }
                HtmlBlock.BlockType.IMAGE -> {
                    addImage(block.block as ImageBlock)
                }
                HtmlBlock.BlockType.VIDEO -> {
                    addVideo(block.block as VideoBlock)
                }
            }
        }
    }

    fun setPreviewText(textBlock: TextBlock) {
        removeAllViews()
        addText(textBlock)
    }

    fun setTextColor(@ColorInt color: Int) {
        for (child in children) {
            if (child is RedditTextView) {
                child.setTextColor(color)
            }
        }
    }

    private fun addText(textBlock: TextBlock) {
        val redditTextView = RedditTextView(context).apply {
            layoutParams = childParams
            text = textBlock.text
            movementMethod = this@RedditView.clickableMovementMethod
        }
        addView(redditTextView)
    }

    private fun addCode(codeBlock: TextBlock) {
        val redditTextView = RedditTextView(context).apply {
            layoutParams = childParams
            text = codeBlock.text
        }
        addView(wrapWithScrollView(redditTextView))
    }

    private fun addTable(tableBlock: TableBlock) {
        addView(wrapWithScrollView(tableBlock.getTableLayout(context, clickableMovementMethod)))
    }

    private fun addImage(imageBlock: ImageBlock) {
        val imageView = AspectRatioImageView(
            context,
            aspectRatios.get(imageBlock.url)
        ).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = imageBlock.altText
            setOnClickListener {
                onLinkClickListener?.onLinkClick(imageBlock.linkUrl)
            }
            setOnLongClickListener {
                val listener = onLinkClickListener ?: return@setOnLongClickListener false
                listener.onLinkLongClick(imageBlock.linkUrl)
                true
            }
        }
        imageView.load(imageBlock.url, blur = false) {
            scale(Scale.FIT)
            error(R.drawable.preview_image_fallback)
            listener(
                onSuccess = { _, result ->
                    val drawable = result.drawable
                    val width = drawable.intrinsicWidth
                    val height = drawable.intrinsicHeight
                    if (width > 0 && height > 0) {
                        val ratio = width.toFloat() / height
                        aspectRatios.put(imageBlock.url, ratio)
                        imageView.aspectRatio = ratio
                    }
                }
            )
        }
        addView(imageView)
    }

    private fun addVideo(videoBlock: VideoBlock) {
        val videoView = AspectRatioVideoView(
            context,
            videoBlock.url,
            aspectRatios.get(videoBlock.url)
        ).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                togglePlayback()
            }
            setOnLongClickListener {
                val listener = onLinkClickListener ?: return@setOnLongClickListener false
                listener.onLinkLongClick(videoBlock.linkUrl)
                true
            }
        }
        addView(videoView)
    }

    private fun wrapWithScrollView(view: View): View {
        return HorizontalScrollView(context).apply {
            layoutParams = childParams
            overScrollMode = OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            addView(view)
        }
    }

    fun setOnLinkClickListener(onLinkClickListener: OnLinkClickListener?) {
        this.onLinkClickListener = onLinkClickListener
    }

    override fun onLinkClick(link: String) {
        onLinkClickListener?.onLinkClick(link)
    }

    override fun onLinkLongClick(link: String) {
        onLinkClickListener?.onLinkLongClick(link)
    }

    override fun onClick() {
        performClick()
    }

    override fun onLongClick() {
        performLongClick()
    }

    private class AspectRatioImageView(
        context: Context,
        initialAspectRatio: Float?
    ) : ImageView(context) {
        var aspectRatio: Float? = initialAspectRatio
            set(value) {
                if (field != value) {
                    field = value
                    requestLayout()
                }
            }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val ratio = aspectRatio
            val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
            val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
            if (ratio == null || ratio <= 0f ||
                widthMode == View.MeasureSpec.UNSPECIFIED || widthSize == 0
            ) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }

            val measuredWidth = resolveSize(widthSize, widthMeasureSpec)
            val contentWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)
            val desiredHeight = (contentWidth / ratio).roundToInt() + paddingTop + paddingBottom
            val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }

    private class AspectRatioVideoView(
        context: Context,
        private val videoUrl: String,
        initialAspectRatio: Float?
    ) : FrameLayout(context) {
        private val playerView = PlayerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            isClickable = false
            isFocusable = false
        }

        private var player: SimpleExoPlayer? = null

        var aspectRatio: Float? = initialAspectRatio
            set(value) {
                if (field != value) {
                    field = value
                    requestLayout()
                }
            }

        init {
            addView(playerView)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            startPlayback()
        }

        override fun onDetachedFromWindow() {
            playerView.player = null
            player?.release()
            player = null
            super.onDetachedFromWindow()
        }

        fun togglePlayback() {
            val currentPlayer = player ?: return
            if (currentPlayer.playWhenReady) {
                currentPlayer.pause()
            } else {
                currentPlayer.play()
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val ratio = aspectRatio
            val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
            val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
            if (ratio == null || ratio <= 0f ||
                widthMode == View.MeasureSpec.UNSPECIFIED || widthSize == 0
            ) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }

            val measuredWidth = resolveSize(widthSize, widthMeasureSpec)
            val contentWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)
            val desiredHeight = (contentWidth / ratio).roundToInt() + paddingTop + paddingBottom
            val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
            super.onMeasure(
                widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY)
            )
        }

        private fun startPlayback() {
            if (player != null) return

            val videoPlayer = SimpleExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width <= 0 || videoSize.height <= 0) return

                        val ratio = videoSize.width * videoSize.pixelWidthHeightRatio /
                            videoSize.height
                        val displayRatio = if (
                            videoSize.unappliedRotationDegrees == 90 ||
                            videoSize.unappliedRotationDegrees == 270
                        ) {
                            1f / ratio
                        } else {
                            ratio
                        }
                        if (displayRatio > 0f) {
                            aspectRatio = displayRatio
                            aspectRatios.put(videoUrl, displayRatio)
                        }
                    }
                })
            }
            player = videoPlayer
            playerView.player = videoPlayer
            videoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
        }
    }

    companion object {
        // Media views are recreated when RecyclerView rebinds a comment.
        private val aspectRatios = LruCache<String, Float>(256)
    }
}
