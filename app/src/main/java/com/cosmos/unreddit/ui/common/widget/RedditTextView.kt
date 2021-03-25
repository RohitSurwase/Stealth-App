package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class RedditTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var scrollEnabled: Boolean = false

    init {
        highlightColor = Color.TRANSPARENT
        isClickable = false
        isFocusable = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    fun setText(text: CharSequence, enableScroll: Boolean) {
        scrollEnabled = enableScroll
        this.text = text
    }

    override fun scrollTo(x: Int, y: Int) {
        if (scrollEnabled) {
            super.scrollTo(x, y)
        }
    }
}
