package com.cosmos.unreddit.ui.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * @see <a href="https://stackoverflow.com/a/43750749">StackOverflow answer</a>
 */
class BreakSpan : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return 0
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = STROKE_WIDTH
        paint.color = COLOR

        val middle = ((top + bottom) / 2).toFloat()

        canvas.drawLine(0f, middle, canvas.width.toFloat(), middle, paint)
    }

    companion object {
        private const val STROKE_WIDTH = 5F
        private const val COLOR = Color.LTGRAY
    }
}
