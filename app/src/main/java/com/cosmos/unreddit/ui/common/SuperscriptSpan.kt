package com.cosmos.unreddit.ui.common

import android.text.TextPaint
import android.text.style.RelativeSizeSpan

class SuperscriptSpan() : RelativeSizeSpan(PROPORTION) {

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.baselineShift += (ds.ascent() / 2).toInt()
    }

    override fun updateMeasureState(ds: TextPaint) {
        super.updateMeasureState(ds)
        ds.baselineShift += (ds.ascent() / 2).toInt()
    }

    companion object {
        private const val PROPORTION = 0.85F
    }
}
