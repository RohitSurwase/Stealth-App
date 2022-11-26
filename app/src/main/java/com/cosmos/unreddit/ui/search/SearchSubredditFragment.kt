package com.cosmos.unreddit.ui.search

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.ui.common.fragment.PagingListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchSubredditFragment : PagingListFragment<SearchSubredditAdapter, SubredditEntity>() {

    override val viewModel: SearchViewModel by hiltNavGraphViewModels(R.id.search)

    override val flow: Flow<PagingData<SubredditEntity>>
        get() = viewModel.subredditDataFlow

    override fun bindViewModel() {
        super.bindViewModel()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lastRefreshSubreddit
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect {
                    setRefreshTime(it)
                }
        }
    }

    override fun createPagingAdapter(): SearchSubredditAdapter {
        return SearchSubredditAdapter { openSubreddit(it) }
    }
}
