package com.cosmos.unreddit.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

class HideBottomViewBehavior<V: View> @JvmOverloads constructor(
    private val leftHandedMode: Boolean = false,
    context: Context? = null,
    attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<V>(context, attrs) {

    private val LINEAR_OUT_SLOW_IN_INTERPOLATOR = FastOutLinearInInterpolator()
    private val FAST_OUT_LINEAR_IN_INTERPOLATOR = LinearOutSlowInInterpolator()

    private var width: Int = 0
    private var currentState: Int = STATE_SCROLLED_UP
    private var currentAnimator: ViewPropertyAnimator? = null

    private val isScrolledUp: Boolean
        get() = currentState == STATE_SCROLLED_UP

    private val isScrolledDown: Boolean
        get() = currentState == STATE_SCROLLED_DOWN

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        val paramsCompat = child.layoutParams as ViewGroup.MarginLayoutParams
        width = child.measuredWidth + paramsCompat.rightMargin
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyConsumed > 0) {
            slideOut(child)
        } else if (dyConsumed < 0) {
            slideIn(child)
        }
    }

    fun slideIn(child: V) {
        slideIn(child, true)
    }

    fun slideIn(child: V, animate: Boolean) {
        if (isScrolledUp) return

        currentAnimator?.let { animator ->
            animator.cancel()
            child.clearAnimation()
        }

        currentState = STATE_SCROLLED_UP
        val targetTranslationX = 0
        if (animate) {
            animateChildTo(
                child,
                targetTranslationX,
                ENTER_ANIMATION_DURATION,
                LINEAR_OUT_SLOW_IN_INTERPOLATOR
            )
        } else {
            child.translationX = targetTranslationX.toFloat()
        }
    }

    fun slideOut(child: V) {
        slideOut(child, true)
    }

    fun slideOut(child: V, animate: Boolean) {
        if (isScrolledDown) return

        currentAnimator?.let { animator ->
            animator.cancel()
            child.clearAnimation()
        }

        currentState = STATE_SCROLLED_DOWN
        val targetTranslationX = if (leftHandedMode) -width else width
        if (animate) {
            animateChildTo(
                child,
                targetTranslationX,
                EXIT_ANIMATION_DURATION,
                FAST_OUT_LINEAR_IN_INTERPOLATOR
            )
        } else {
            child.translationX = targetTranslationX.toFloat()
        }
    }

    private fun animateChildTo(
        child: V,
        targetX: Int,
        duration: Long,
        interpolator: TimeInterpolator
    ) {
        currentAnimator = child
            .animate()
            .translationX(targetX.toFloat())
            .setInterpolator(interpolator)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimator = null
                }
            })
    }

    companion object {
        private const val ENTER_ANIMATION_DURATION: Long = 225L
        private const val EXIT_ANIMATION_DURATION: Long = 175L

        private const val STATE_SCROLLED_DOWN: Int = 1
        private const val STATE_SCROLLED_UP: Int = 2
    }
}
