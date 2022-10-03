package com.cosmos.unreddit.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavOptions
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.databinding.IncludeLoadingStateBinding
import com.cosmos.unreddit.databinding.ItemListContentBinding
import com.cosmos.unreddit.ui.commentmenu.CommentMenuFragment
import com.cosmos.unreddit.ui.postdetails.PostDetailsFragment
import com.cosmos.unreddit.ui.sort.SortFragment
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

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

fun Fragment.clearSortingListener() {
    childFragmentManager.clearFragmentResultListener(SortFragment.REQUEST_KEY_SORTING)
}

fun Fragment.setCommentListener(result: (Comment.CommentEntity?) -> Unit) {
    childFragmentManager.setFragmentResultListener(
        CommentMenuFragment.REQUEST_KEY_COMMENT,
        viewLifecycleOwner
    ) { _, bundle ->
        val comment = bundle.getParcelable(CommentMenuFragment.BUNDLE_KEY_COMMENT)
                as? Comment.CommentEntity
        result(comment)
    }
}

fun Fragment.clearCommentListener() {
    childFragmentManager.clearFragmentResultListener(CommentMenuFragment.REQUEST_KEY_COMMENT)
}

fun Fragment.setNavigationListener(result: (Boolean) -> Unit) {
    setFragmentResultListener(PostDetailsFragment.REQUEST_KEY_NAVIGATION) { _, bundle ->
        val showNavigation = bundle.getBoolean(PostDetailsFragment.BUNDLE_KEY_NAVIGATION)
        result(showNavigation)
    }
}

fun Fragment.clearNavigationListener() {
    clearFragmentResultListener(PostDetailsFragment.REQUEST_KEY_NAVIGATION)
}

fun Fragment.openExternalLink(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    val packageManager = activity?.packageManager ?: return

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

fun Fragment.shareExternalLink(url: String, title: String? = null) {
    val share = Intent.createChooser(
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)

            title?.let {
                putExtra(Intent.EXTRA_TITLE, it)
            }
        },
        null
    )
    startActivity(share)
}

fun Fragment.getNavOptions(): NavOptions {
    return NavOptions.Builder()
        .setEnterAnim(R.anim.nav_enter_anim)
        .setExitAnim(R.anim.nav_exit_anim)
        .setPopEnterAnim(R.anim.nav_enter_anim)
        .setPopExitAnim(R.anim.nav_exit_anim)
        .build()
}

fun DialogFragment.doAndDismiss(block: () -> Unit) {
    block()
    dismiss()
}

suspend fun PagingDataAdapter<out Any, out RecyclerView.ViewHolder>.onRefreshFromNetwork(
    onRefresh: () -> Unit
) {
    loadStateFlow.distinctUntilChangedBy { it.refresh }
        .filter { it.refresh is LoadState.NotLoading }
        .drop(1)
        .collect { onRefresh() }
}

fun PagingDataAdapter<out Any, out RecyclerView.ViewHolder>.isEmpty(): Boolean {
    return itemCount == 0
}

fun PagingDataAdapter<out Any, out RecyclerView.ViewHolder>.addLoadStateListener(
    list: RecyclerView,
    binding: IncludeLoadingStateBinding,
    onError: () -> Unit
) {
    addLoadStateListener { loadState ->
        list.visibility = when (loadState.source.refresh) {
            is LoadState.NotLoading -> View.VISIBLE
            else -> View.INVISIBLE // Set to INVISIBLE to keep MotionLayout gestures
        }

        binding.loadingCradle.isVisible = loadState.source.refresh is LoadState.Loading

        val errorState = loadState.source.refresh as? LoadState.Error
        errorState?.let {
            onError.invoke()
        }

        // TODO: Animation
        val noData = loadState.source.refresh is LoadState.NotLoading &&
                loadState.append.endOfPaginationReached &&
                this.isEmpty()
        binding.emptyData.isVisible = noData
        binding.textEmptyData.isVisible = noData
    }
}

fun ViewPager2.getRecyclerView(): RecyclerView? {
    return getChildAt(0) as? RecyclerView
}

fun ViewPager2.getItemView(position: Int): View? {
    return getRecyclerView()?.findViewHolderForAdapterPosition(position)?.itemView
}

fun ViewPager2.getListContent(position: Int): ItemListContentBinding? {
    val viewGroup = getItemView(position) as? ViewGroup
    return viewGroup?.children?.firstOrNull()?.let { ItemListContentBinding.bind(it) }
}

fun ViewPager2.scrollToTop(position: Int) {
    val viewGroup = getItemView(position) as? ViewGroup
    viewGroup?.children?.firstOrNull()?.let {
        ItemListContentBinding.bind(it).apply {
            listContent.betterSmoothScrollToPosition(0)
        }
    }
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
            val roundedSubscribers = String.format("%.1f", this.div(1000f))
            "${roundedSubscribers}k"
        }
        else -> {
            val roundedSubscribers = String.format("%.1f", this.div(1_000_000f))
            "${roundedSubscribers}m"
        }
    }
}

fun MimeTypeMap.getMimeTypeFromUrl(url: String): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    return getMimeTypeFromExtension(extension)
}
