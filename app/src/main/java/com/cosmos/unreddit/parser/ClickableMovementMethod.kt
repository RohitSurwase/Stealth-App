package com.cosmos.unreddit.parser

import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.os.postDelayed

class ClickableMovementMethod(
    private val clickListener: OnClickListener
) : LinkMovementMethod() {

    interface OnClickListener {
        fun onLinkClick(link: String)

        fun onLinkLongClick(link: String)

        fun onClick()

        fun onLongClick()
    }

    private val longClickHandler = Handler(Looper.getMainLooper())

    private var longClicked = false

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            var x = event.x.toInt()
            var y = event.y.toInt()

            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop

            x += widget.scrollX
            y += widget.scrollY

            val layout = widget.layout

            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            val links = buffer.getSpans(off, off, URLSpan::class.java)
            val spoilers = buffer.getSpans(off, off, SpoilerSpan::class.java)

            if (links.isNotEmpty()) {
                val url = links[0].url

                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        longClickHandler.postDelayed(
                            ViewConfiguration.getLongPressTimeout().toLong()
                        ) {
                            clickListener.onLinkLongClick(url)
                            longClicked = true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        longClickHandler.removeCallbacksAndMessages(null)

                        if (!longClicked) {
                            clickListener.onLinkClick(url)
                        }

                        longClicked = false
                    }
                }

                return true
            } else if (spoilers.isEmpty()) {
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        longClickHandler.postDelayed(
                            ViewConfiguration.getLongPressTimeout().toLong()
                        ) {
                            clickListener.onLongClick()
                            longClicked = true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        longClickHandler.removeCallbacksAndMessages(null)

                        if (!longClicked) {
                            clickListener.onClick()
                        }

                        longClicked = false
                    }
                }

                return true
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            longClickHandler.removeCallbacksAndMessages(null)
        }

        return super.onTouchEvent(widget, buffer, event)
    }
}
