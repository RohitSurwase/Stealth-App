package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import com.cosmos.unreddit.data.model.HtmlBlock
import com.cosmos.unreddit.data.model.RedditText
import com.cosmos.unreddit.data.model.TableBlock
import com.cosmos.unreddit.data.model.TextBlock
import com.cosmos.unreddit.util.ClickableMovementMethod

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
        addView(wrapWithScrollView(tableBlock.getTableLayout(context)))
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
}
