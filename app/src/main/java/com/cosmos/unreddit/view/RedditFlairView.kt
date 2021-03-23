package com.cosmos.unreddit.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.model.Flair

class RedditFlairView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private val childParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)

    private val flairImageSize: Int by lazy {
        context.resources.getDimension(R.dimen.flair_image_size).toInt()
    }

    init {
        orientation = HORIZONTAL
    }

    fun setFlair(flair: Flair) {
        removeAllViews()

        for (data in flair.data) {
            when (data.second) {
                Flair.FlairType.TEXT -> {
                    val textView = TextView(
                        context,
                        null,
                        0,
                        R.style.TextAppearancePostFlair
                    ).apply {
                        layoutParams = childParams
                        setSingleLine()
                        isHorizontalFadingEdgeEnabled = true
                        text = data.first
                    }
                    addView(textView)
                }
                Flair.FlairType.IMAGE -> {
                    val imageView = ImageView(context).apply {
                        layoutParams = LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            flairImageSize
                        )
                    }
                    imageView.load(data.first) {
                        crossfade(true)
                        scale(Scale.FIT)
                        precision(Precision.AUTOMATIC)
                    }
                    addView(imageView)
                }
            }
        }
    }
}
