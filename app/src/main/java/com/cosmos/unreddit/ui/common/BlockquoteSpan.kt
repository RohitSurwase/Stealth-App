package com.cosmos.unreddit.ui.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan

class BlockquoteSpan : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return STRIPE_WIDTH_PX + GAP_WIDTH_PX
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        val style = p.style
        val color = p.color

        p.style = Paint.Style.FILL
        p.color = Color.LTGRAY

        c.drawRect(
            x.toFloat(),
            top.toFloat(),
            (x + dir * STRIPE_WIDTH_PX).toFloat(),
            bottom.toFloat(),
            p
        )

        p.style = style
        p.color = color
    }

    companion object {
        private const val STRIPE_WIDTH_PX = 2
        private const val GAP_WIDTH_PX = 16
    }
}
