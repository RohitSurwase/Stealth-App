package com.cosmos.unreddit.util.extension

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.BlurTransformation
import com.cosmos.unreddit.R
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

        view.updatePadding(
            left = paddingLeft,
            top = paddingTop,
            right = paddingRight,
            bottom = paddingBottom
        )

        windowInsets
    }
}

fun View.showWindowInsets(
    show: Boolean,
    @InsetsType types: Int = WindowInsetsCompat.Type.systemBars(),
    behavior: Int = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
) {
    ViewCompat.getWindowInsetsController(this)?.let { windowInsetsController ->
        windowInsetsController.systemBarsBehavior = behavior
        if (show) {
            windowInsetsController.show(types)
        } else {
            windowInsetsController.hide(types)
        }
    }
}
