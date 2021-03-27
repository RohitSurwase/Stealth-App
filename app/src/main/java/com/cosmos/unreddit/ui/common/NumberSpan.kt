/*
 * Copyright (C) 2017 Dominik Mosberger <https://github.com/mosberger>
 * Copyright (C) 2013 Leszek Mzyk <https://github.com/imbryk>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cosmos.unreddit.ui.common

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.text.Layout
import android.text.Spanned
import android.text.style.BulletSpan

/**
 * Class to use Numbered Lists in TextViews.
 * The span works the same as [android.text.style.BulletSpan] and all lines of the entry have
 * the same leading margin.
 */
class NumberSpan(gapWidth: Int, number: Int) : BulletSpan(gapWidth) {

    private val mNumberGapWidth: Int = gapWidth
    private val mNumber: String = "$number."

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(mNumberGapWidth)
        dest.writeString(mNumber)
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return 2 * STANDARD_GAP_WIDTH + mNumberGapWidth
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        l: Layout?
    ) {
        if ((text as Spanned).getSpanStart(this) == start) {
            val style = p.style
            p.style = Paint.Style.FILL
            if (c.isHardwareAccelerated) {
                c.save()
                c.drawText(mNumber, (x + dir).toFloat(), baseline.toFloat(), p)
                c.restore()
            } else {
                c.drawText(mNumber, (x + dir).toFloat(), (top + bottom) / 2.0f, p)
            }
            p.style = style
        }
    }

    companion object {
        const val STANDARD_GAP_WIDTH = 10
    }
}
