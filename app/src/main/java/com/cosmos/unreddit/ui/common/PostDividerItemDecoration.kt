package com.cosmos.unreddit.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.cosmos.unreddit.R

class PostDividerItemDecoration(
    context: Context,
    orientation: Int = VERTICAL
) : DividerItemDecoration(context, orientation) {

    init {
        ContextCompat.getDrawable(context, R.drawable.post_divider)?.let {
            setDrawable(it)
        }
    }
}
