package com.cosmos.unreddit.view

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
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.post.Sorting

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
                RedditApi.TimeSorting.HOUR -> context.getString(R.string.sort_time_hour_short)
                RedditApi.TimeSorting.DAY -> context.getString(R.string.sort_time_day_short)
                RedditApi.TimeSorting.WEEK -> context.getString(R.string.sort_time_week_short)
                RedditApi.TimeSorting.MONTH -> context.getString(R.string.sort_time_month_short)
                RedditApi.TimeSorting.YEAR -> context.getString(R.string.sort_time_year_short)
                RedditApi.TimeSorting.ALL -> context.getString(R.string.sort_time_all_short)
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
            RedditApi.Sort.HOT -> R.drawable.ic_hot
            RedditApi.Sort.NEW -> R.drawable.ic_new
            RedditApi.Sort.TOP -> R.drawable.ic_top
            RedditApi.Sort.RISING -> R.drawable.ic_rising
            RedditApi.Sort.CONTROVERSIAL -> R.drawable.ic_controversial
            RedditApi.Sort.RELEVANCE -> R.drawable.ic_relevance
            RedditApi.Sort.COMMENTS -> R.drawable.ic_comments
            RedditApi.Sort.BEST -> R.drawable.ic_best
            RedditApi.Sort.OLD -> R.drawable.ic_old
            RedditApi.Sort.QA -> R.drawable.ic_question_answer
        }
    }

    private fun getIconVisibility(sorting: Sorting): Int {
        val shouldBeVisible = when (sortType) {
            SortType.GENERAL -> sorting.generalSorting != RedditApi.Sort.HOT
            SortType.SEARCH, SortType.USER -> true
            SortType.POST -> sorting.generalSorting != RedditApi.Sort.BEST
        }
        return if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun getTextVisibility(sorting: Sorting): Int {
        val shouldBeVisible = when (sortType) {
            SortType.GENERAL, SortType.USER ->
                sorting.generalSorting == RedditApi.Sort.TOP ||
                    sorting.generalSorting == RedditApi.Sort.CONTROVERSIAL
            SortType.SEARCH ->
                sorting.generalSorting == RedditApi.Sort.TOP ||
                    sorting.generalSorting == RedditApi.Sort.RELEVANCE ||
                    sorting.generalSorting == RedditApi.Sort.COMMENTS
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
