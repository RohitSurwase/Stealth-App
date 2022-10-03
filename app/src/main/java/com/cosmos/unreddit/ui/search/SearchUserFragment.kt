package com.cosmos.unreddit.ui.search

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.paging.PagingData
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.ui.common.fragment.PagingListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

@AndroidEntryPoint
class SearchUserFragment : PagingListFragment<SearchUserAdapter, User>() {

    override val viewModel: SearchViewModel by hiltNavGraphViewModels(R.id.search)

    override val flow: Flow<PagingData<User>>
        get() = viewModel.userDataFlow

    override fun createPagingAdapter(): SearchUserAdapter {
        return SearchUserAdapter { openUser(it) }
    }
}
