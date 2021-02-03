package com.cosmos.unreddit.search

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.database.UserMapper
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.user.User

class SearchUserDataSource(
    private val redditApi: RedditApi,
    private val query: String,
    private val sorting: Sorting
) : PagingSource<String, User>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, User> {
        return try {
            val response = redditApi.searchUser(
                query,
                sorting.generalSorting,
                sorting.timeSorting,
                params.key
            )
            val data = response.data

            val items = UserMapper.dataToEntities(data.children)

            LoadResult.Page(items, data.before, data.after)
        } catch (e: Exception) {
            Log.e("SearchUserDataSource", "Error", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, User>): String? {
        return null
    }
}
