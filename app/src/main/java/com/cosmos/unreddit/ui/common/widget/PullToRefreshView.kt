package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.cosmos.unreddit.R

class PullToRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), PullToRefreshLayout.RefreshCallback {

    private var lastRefresh: TextView

    private var cradleView: CradleView

    init {
        inflate(context, R.layout.view_pull_refresh, this)

        cradleView = findViewById(R.id.loading_cradle_pull)
        lastRefresh = findViewById(R.id.text_last_refresh)
    }

    fun setLastRefresh(text: CharSequence) {
        lastRefresh.text = text
    }

    override fun reset() {
        cradleView.run {
            stop()
            reset()
        }
    }

    override fun refreshing() {
        cradleView.start()
    }

    override fun refreshComplete() {
        reset()
    }

    override fun pullToRefresh() {

    }

    override fun releaseToRefresh() {

    }

    override fun pullProgress(pullDistance: Float, pullProgress: Float) {

    }
}
