package com.cosmos.unreddit.util

import android.graphics.LinearGradient
import android.graphics.Shader
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
