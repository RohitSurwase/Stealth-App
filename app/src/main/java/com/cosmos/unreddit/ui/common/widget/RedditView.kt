package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.util.LruCache
import android.view.View
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
            imageAspectRatios.get(imageBlock.url)
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
                        imageAspectRatios.put(imageBlock.url, ratio)
                        imageView.aspectRatio = ratio
                    }
                }
            )
        }
        addView(imageView)
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

    companion object {
        // Image views are recreated when RecyclerView rebinds a comment.
        private val imageAspectRatios = LruCache<String, Float>(256)
    }
}
