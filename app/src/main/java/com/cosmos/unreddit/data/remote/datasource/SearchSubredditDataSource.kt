package com.cosmos.unreddit.data.remote.datasource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.local.mapper.SubredditMapper
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi

class SearchSubredditDataSource(
    private val redditApi: RedditApi,
    private val query: String,
    private val sorting: Sorting
) : PagingSource<String, SubredditEntity>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, SubredditEntity> {
        return try {
            val response = redditApi.searchSubreddit(
                query,
                sorting.generalSorting,
                sorting.timeSorting,
                params.key
            )
            val data = response.data

            val items = SubredditMapper.dataToEntities(data.children)

            LoadResult.Page(items, data.before, data.after)
        } catch (e: Exception) {
            Log.e("SubredditDataSource", "Error", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, SubredditEntity>): String? {
        return null
    }
}
