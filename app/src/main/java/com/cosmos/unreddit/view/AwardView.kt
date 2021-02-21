package com.cosmos.unreddit.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.post.Award

class AwardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private val awardImageSize by lazy {
        context.resources.getDimension(R.dimen.award_image_size).toInt()
    }
    private val overlapMargin by lazy {
        context.resources.getDimension(R.dimen.award_overlap_margin).toInt()
    }
    private val countMargin by lazy {
        context.resources.getDimension(R.dimen.award_count_margin).toInt()
    }
    private val countImageMargin by lazy {
        context.resources.getDimension(R.dimen.award_count_image_margin).toInt()
    }

    init {
        background = ContextCompat.getDrawable(context, R.drawable.award_background)
        val paddingVertical = context.resources.getDimension(
            R.dimen.award_chip_padding_vertical
        ).toInt()
        val paddingHorizontal = context.resources.getDimension(
            R.dimen.award_chip_padding_horizontal
        ).toInt()
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        orientation = HORIZONTAL
    }

    fun setAwards(awards: List<Award>, totalAwards: Int) {
        removeAllViews()

        when {
            awards.size <= AWARD_LIMIT -> {
                for (i in awards.indices) {
                    val textView = getTextView(
                        context.getString(R.string.award_count, awards[i].count),
                        i > 0
                    )

                    Coil.imageLoader(context).enqueue(
                        ImageRequest.Builder(context).apply {
                            data(awards[i].icon)
                            allowHardware(false)
                            target { drawable ->
                                drawable.setBounds(0, 0, awardImageSize, awardImageSize)
                                textView.apply {
                                    setCompoundDrawables(drawable, null, null, null)
                                    compoundDrawablePadding = countImageMargin
                                }
                            }
                        }.build()
                    )

                    addView(textView)
                }
            }
            else -> {
                for (i in 0 until AWARD_LIMIT) {
                    val imageView = ImageView(context).apply {
                        layoutParams = LayoutParams(awardImageSize, awardImageSize).apply {
                            if (i > 0) {
                                marginStart = overlapMargin
                            }
                        }
                    }

                    imageView.load(awards[i].icon) {
                        crossfade(true)
                        scale(Scale.FIT)
                        precision(Precision.AUTOMATIC)
                    }

                    addView(imageView)
                }

                val textView = getTextView(
                    context.getString(R.string.award_count, totalAwards),
                    true
                )

                addView(textView)
            }
        }
    }

    private fun getTextView(text: String, withMargin: Boolean): TextView {
        return TextView(
            context,
            null,
            0,
            R.style.TextAppearanceAward
        ).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                if (withMargin) {
                    marginStart = countMargin
                }
            }
            this.text = text
        }
    }

    companion object {
        private const val AWARD_LIMIT = 3
    }
}
