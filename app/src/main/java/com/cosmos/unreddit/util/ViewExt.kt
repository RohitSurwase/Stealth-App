package com.cosmos.unreddit.util

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt

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
