package com.cosmos.unreddit.view

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

    init {
        setImageDrawable(cradleDrawable)

        cradleDrawable?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                cradleDrawable.start()
            }
        })
    }

    fun start() {
        cradleDrawable?.start()
    }

    fun stop() {
        cradleDrawable?.stop()
    }
}
