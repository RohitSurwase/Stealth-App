package com.cosmos.unreddit.util

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.BlurTransformation

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
        .apply(builder)
        .apply {
            if (blur) {
                transformations(BlurTransformation(context, radius, sampling))
            }
        }
        .build()
    context.imageLoader.enqueue(request)
}
