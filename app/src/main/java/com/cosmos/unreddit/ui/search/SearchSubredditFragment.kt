package com.cosmos.unreddit.ui.search

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.paging.PagingData
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.ui.common.fragment.PagingListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

@AndroidEntryPoint
class SearchSubredditFragment : PagingListFragment<SearchSubredditAdapter, SubredditEntity>() {

    override val viewModel: SearchViewModel by hiltNavGraphViewModels(R.id.search)

    override val flow: Flow<PagingData<SubredditEntity>>
        get() = viewModel.subredditDataFlow

    override fun createPagingAdapter(): SearchSubredditAdapter {
        return SearchSubredditAdapter { openSubreddit(it) }
    }
}
