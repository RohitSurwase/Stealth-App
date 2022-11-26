package com.cosmos.unreddit.ui.common.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.cosmos.unreddit.ui.common.widget.PullToRefreshLayout
import com.cosmos.unreddit.ui.loadstate.NetworkLoadStateAdapter
import com.cosmos.unreddit.util.extension.addLoadStateListener
import com.cosmos.unreddit.util.extension.betterSmoothScrollToPosition
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.onRefreshFromNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class PagingListFragment<T : PagingDataAdapter<R, out ViewHolder>, R : Any>
    : ListFragment<T>() {

    protected abstract val flow: Flow<PagingData<R>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
        binding.loadingState.infoRetry.setActionClickListener { adapter.retry() }
    }

    protected open fun bindViewModel() {
        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                flow.collectLatest {
                    adapter.submitData(it)
                }
            }

            launch {
                adapter.onRefreshFromNetwork {
                    binding.listContent.betterSmoothScrollToPosition(0)
                }
            }
        }
    }

    final override fun createAdapter(): T {
        return createPagingAdapter().apply {
            addLoadStateListener(binding.listContent, binding.loadingState, binding.pullRefresh) {
                showRetryBar()
            }
            withLoadStateHeaderAndFooter(
                header = NetworkLoadStateAdapter { adapter.retry() },
                footer = NetworkLoadStateAdapter { adapter.retry() }
            )
        }
    }

    override fun onRefresh() {
        adapter.refresh()
    }

    protected abstract fun createPagingAdapter(): T

    override fun onDestroyView() {
        (binding.pullRefresh.refreshView as? PullToRefreshLayout.RefreshCallback)?.reset()
        super.onDestroyView()
    }
}
