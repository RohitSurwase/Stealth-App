package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.TimeSorting

class SortIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var icon: ImageView
    private var text: TextView

    private var sortType: SortType = SortType.GENERAL

    private val popInAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.pop_in) }
    private val popOutAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.pop_out) }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SortIconView,
            0, 0
        ).apply {
            try {
                val sortTypeValue = getInteger(
                    R.styleable.SortIconView_sortType,
                    SortType.GENERAL.value
                )
                sortType = SortType.fromValue(sortTypeValue)
            } finally {
                recycle()
            }
        }

        inflate(context, R.layout.view_sort_icon, this)

        icon = findViewById(R.id.icon)
        text = findViewById(R.id.text)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        clipToPadding = false
    }

    fun setSorting(sorting: Sorting) {
        with(icon) {
            visibility = getIconVisibility(sorting)

            if (isVisible) {
                setImageResource(getIconResDrawable(sorting))
                startAnimation(popInAnimation)
            } else {
                startAnimation(popOutAnimation)
            }
        }

        with(text) {
            val showOutAnimation = isVisible

            visibility = getTextVisibility(sorting)

            text = when (sorting.timeSorting) {
                TimeSorting.HOUR -> context.getString(R.string.sort_time_hour_short)
                TimeSorting.DAY -> context.getString(R.string.sort_time_day_short)
                TimeSorting.WEEK -> context.getString(R.string.sort_time_week_short)
                TimeSorting.MONTH -> context.getString(R.string.sort_time_month_short)
                TimeSorting.YEAR -> context.getString(R.string.sort_time_year_short)
                TimeSorting.ALL -> context.getString(R.string.sort_time_all_short)
                null -> {
                    if (showOutAnimation) startAnimation(popOutAnimation)
                    return@with
                }
            }

            startAnimation(popInAnimation)
        }
    }

    @DrawableRes
    private fun getIconResDrawable(sorting: Sorting): Int {
        return when (sorting.generalSorting) {
            Sort.HOT -> R.drawable.ic_hot
            Sort.NEW -> R.drawable.ic_new
            Sort.TOP -> R.drawable.ic_top
            Sort.RISING -> R.drawable.ic_rising
            Sort.CONTROVERSIAL -> R.drawable.ic_controversial
            Sort.RELEVANCE -> R.drawable.ic_relevance
            Sort.COMMENTS -> R.drawable.ic_comments
            Sort.BEST -> R.drawable.ic_best
            Sort.OLD -> R.drawable.ic_old
            Sort.QA -> R.drawable.ic_question_answer
        }
    }

    private fun getIconVisibility(sorting: Sorting): Int {
        val shouldBeVisible = when (sortType) {
            SortType.GENERAL -> sorting.generalSorting != Sort.HOT
            SortType.SEARCH, SortType.USER -> true
            SortType.POST -> sorting.generalSorting != Sort.BEST
        }
        return if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun getTextVisibility(sorting: Sorting): Int {
        val shouldBeVisible = when (sortType) {
            SortType.GENERAL, SortType.USER ->
                sorting.generalSorting == Sort.TOP ||
                    sorting.generalSorting == Sort.CONTROVERSIAL
            SortType.SEARCH ->
                sorting.generalSorting == Sort.TOP ||
                    sorting.generalSorting == Sort.RELEVANCE ||
                    sorting.generalSorting == Sort.COMMENTS
            SortType.POST -> false
        }
        return if (shouldBeVisible) View.VISIBLE else View.GONE
    }

    private enum class SortType(val value: Int) {
        GENERAL(0), SEARCH(1), USER(2), POST(3);

        companion object {
            fun fromValue(value: Int): SortType {
                return when (value) {
                    0 -> GENERAL
                    1 -> SEARCH
                    2 -> USER
                    3 -> POST
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }
}
