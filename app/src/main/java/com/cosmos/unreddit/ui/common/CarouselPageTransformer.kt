package com.cosmos.unreddit.ui.common

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Adapted from [this SO answer](https://stackoverflow.com/a/48951676)
 */
class CarouselPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width
        when {
            position < -1 -> {
                page.alpha = MIN_FADE
            }
            position < 0 -> {
                page.translationZ = position
                page.alpha = 1 + position * (1 - MIN_FADE)
                page.translationX = -pageWidth * TRANSLATION_FACTOR * position
                val scaleFactor = (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - abs(position)))
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
            }
            position == 0F -> {
                page.alpha = 1F
                page.translationX = 0F
                page.translationZ = 0F
                page.scaleX = MAX_SCALE
                page.scaleY = MAX_SCALE
            }
            position <= 1 -> {
                page.translationZ = -position
                page.alpha = 1 - position * (1 - MIN_FADE)
                page.translationX = pageWidth * TRANSLATION_FACTOR * -position
                val scaleFactor = (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - abs(position)))
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
            }
            else -> {
                page.alpha = MIN_FADE
            }
        }
    }

    companion object {
        private const val MIN_SCALE = 0.5F
        private const val MAX_SCALE = 1F
        private const val MIN_FADE = 0.2F
        private const val TRANSLATION_FACTOR = 0.75F
    }
}
