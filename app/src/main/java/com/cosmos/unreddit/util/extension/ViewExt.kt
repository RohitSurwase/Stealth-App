package com.cosmos.unreddit.util.extension

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.util.BlurTransformation
import com.google.android.material.textfield.TextInputLayout

fun TextView.applyGradient(text: String, @ColorInt colors: IntArray) {
    val width = paint.measureText(text)
    val gradientShader = LinearGradient(
        0F,
        0F,
        width,
        textSize,
        colors,
        null,
        Shader.TileMode.CLAMP
    )
    paint.shader = gradientShader
}

fun TextView.blurText(blur: Boolean, coefficient: Int = 3) {
    if (blur) {
        val radius = textSize / coefficient
        val blurMaskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paint.maskFilter = blurMaskFilter
    } else {
        paint.maskFilter = null
    }
}

fun ImageView.load(
    data: Any?,
    blur: Boolean,
    radius: Float = 25F,
    sampling: Float = 4F,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .crossfade(true)
        .scale(Scale.FILL)
        .precision(Precision.AUTOMATIC)
        .placeholder(R.drawable.image_placeholder)
        .apply(builder)
        .apply {
            if (blur) {
                transformations(BlurTransformation(context, radius, sampling))
            }
        }
        .build()
    context.imageLoader.enqueue(request)
}

fun TextInputLayout.text(): String? {
    return editText?.text?.toString()
}

fun View.applyWindowInsets(
    left: Boolean = true,
    top: Boolean = true,
    right: Boolean = true,
    bottom: Boolean = true
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        val paddingLeft = if (left) insets.left else view.paddingLeft
        val paddingTop = if (top) insets.top else view.paddingTop
        val paddingRight = if (right) insets.right else view.paddingRight
        val paddingBottom = if (bottom) insets.bottom else view.paddingBottom

        view.run {
            updatePadding(
                left = paddingLeft,
                top = paddingTop,
                right = paddingRight,
                bottom = paddingBottom
            )

            clearWindowInsetsListener()
        }

        windowInsets
    }
}

fun View.clearWindowInsetsListener() {
    ViewCompat.setOnApplyWindowInsetsListener(this, null)
}

fun View.applyMarginWindowInsets(
    left: Boolean = true,
    top: Boolean = true,
    right: Boolean = true,
    bottom: Boolean = true
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        view.run {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (left) leftMargin += insets.left
                if (top) topMargin += insets.top
                if (right) rightMargin += insets.right
                if (bottom) bottomMargin += insets.bottom
            }

            clearWindowInsetsListener()
        }

        windowInsets
    }
}

fun View.showWithAlpha(show: Boolean, duration: Long) {
    val fromAlpha = if (show) 0F else 1F
    val toAlpha = if (show) 1F else 0F

    alpha = fromAlpha

    animate()
        .alpha(toAlpha)
        .withStartAction {
            if (show) {
                isVisible = true
            }
        }
        .withEndAction {
            if (!show) {
                isVisible = false
            }
        }
        .setDuration(duration)
        .start()
}

val Int.asBoolean: Boolean
    get() = this == View.VISIBLE
