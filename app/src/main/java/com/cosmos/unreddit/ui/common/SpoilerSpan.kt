package com.cosmos.unreddit.ui.common

import android.graphics.BlurMaskFilter
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class SpoilerSpan() : ClickableSpan() {

    private var showSpoiler = false

    override fun onClick(widget: View) {
        showSpoiler = !showSpoiler
        widget.invalidate()
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.maskFilter = if (!showSpoiler) {
            BlurMaskFilter((ds.textSize / COEFFICIENT), BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
    }

    companion object {
        private const val COEFFICIENT = 3
    }
}
