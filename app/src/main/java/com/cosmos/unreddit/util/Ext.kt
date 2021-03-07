package com.cosmos.unreddit.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.sort.SortFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlin.math.round

fun Double.getPercentageValue(start: Int, end: Int) = end * this + start * (1 - this)

fun Activity.setStatusBarColor(color: Int) {
    with(window) {
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        statusBarColor = color
    }
}

fun View.showSoftKeyboard() {
    requestFocus()
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideSoftKeyboard() {
    requestFocus()
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun ImageView.loadSubredditIcon(uri: String?) {
    this.load(uri) {
        crossfade(true)
        scale(Scale.FILL)
        precision(Precision.AUTOMATIC)
        placeholder(R.drawable.icon_reddit_placeholder)
        error(R.drawable.icon_reddit_placeholder)
        fallback(R.drawable.icon_reddit_placeholder)
    }
}

fun Fragment.setSortingListener(result: (Sorting?) -> Unit) {
    childFragmentManager.setFragmentResultListener(
        SortFragment.REQUEST_KEY_SORTING,
        viewLifecycleOwner
    ) { _, bundle ->
        val sorting = bundle.getParcelable(SortFragment.BUNDLE_KEY_SORTING) as? Sorting
        result(sorting)
    }
}

fun Fragment.openExternalLink(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    val packageManager = activity?.packageManager ?: return

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

suspend fun PagingDataAdapter<out Any, out RecyclerView.ViewHolder>.onRefreshFromNetwork(
    onRefresh: () -> Unit
) {
    loadStateFlow.distinctUntilChangedBy { it.refresh }
        .filter { it.refresh is LoadState.NotLoading }
        .collect { onRefresh() }
}

fun ViewPager2.getRecyclerView(): RecyclerView? {
    return getChildAt(0) as? RecyclerView
}

fun ViewPager2.getItemView(position: Int): View? {
    return getRecyclerView()?.findViewHolderForAdapterPosition(position)?.itemView
}

// https://carlrice.io/blog/better-smoothscrollto
fun RecyclerView.betterSmoothScrollToPosition(targetItem: Int) {
    layoutManager?.apply {
        val maxScroll = 10
        when (this) {
            is LinearLayoutManager -> {
                val topItem = findFirstVisibleItemPosition()
                val distance = topItem - targetItem
                val anchorItem = when {
                    distance > maxScroll -> targetItem + maxScroll
                    distance < -maxScroll -> targetItem - maxScroll
                    else -> topItem
                }
                if (anchorItem != topItem) scrollToPosition(anchorItem)
                post {
                    smoothScrollToPosition(targetItem)
                }
            }
            else -> smoothScrollToPosition(targetItem)
        }
    }
}

fun Int?.formatNumber(): String {
    return when {
        this == null -> "" // TODO
        this < 1000 -> this.toString()
        this < 1_000_000 -> {
            val roundedSubscribers = round(this.div(1000f)).toInt()
            "${roundedSubscribers}k"
        }
        else -> {
            val roundedSubscribers = String.format("%.1f", this.div(1_000_000f))
            "${roundedSubscribers}m"
        }
    }
}
