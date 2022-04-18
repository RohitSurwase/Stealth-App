package com.cosmos.unreddit.util.extension

import android.content.Context
import android.util.TypedValue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun Context.toPixels(value: Number, unit: Int = TypedValue.COMPLEX_UNIT_DIP): Float {
    return TypedValue.applyDimension(unit, value.toFloat(), resources.displayMetrics)
}

fun Long.toMillis(): Long {
    return TimeUnit.SECONDS.toMillis(this)
}

fun Int.fitToRange(range: IntRange): Int = abs(
    this - ((this / (range.last - range.first)) * (range.last - range.first))
)
