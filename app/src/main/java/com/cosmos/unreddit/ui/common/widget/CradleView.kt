package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.cosmos.unreddit.R

class CradleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = kotlin.run {
                if (value) {
                    start()
                    View.VISIBLE
                } else {
                    stop()
                    View.GONE
                }
            }
        }

    private val cradleDrawable = AnimatedVectorDrawableCompat.create(
        context,
        R.drawable.cradle_anim
    )

    private val callback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            post { cradleDrawable?.start() }
        }
    }

    init {
        setImageDrawable(cradleDrawable)
    }

    fun start() {
        registerCallback()
        cradleDrawable?.start()
    }

    fun stop() {
        cradleDrawable?.stop()
        unregisterCallback()
    }

    private fun registerCallback() {
        cradleDrawable?.registerAnimationCallback(callback)
    }

    private fun unregisterCallback() {
        cradleDrawable?.unregisterAnimationCallback(callback)
    }
}
