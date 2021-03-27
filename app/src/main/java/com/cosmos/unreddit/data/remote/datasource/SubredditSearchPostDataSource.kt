package com.cosmos.unreddit.data.remote.datasource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.local.mapper.PostMapper
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi

class SubredditSearchPostDataSource(
    private val redditApi: RedditApi,
    private val subreddit: String,
    private val query: String,
    private val sorting: Sorting
) : PagingSource<String, PostEntity>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, PostEntity> {
        return try {
            val response = redditApi.searchInSubreddit(
                subreddit,
                query,
                sorting.generalSorting,
                sorting.timeSorting,
                params.key
            )
            val data = response.data

            val items = PostMapper.dataToEntities(data.children)

            LoadResult.Page(items, data.before, data.after)
        } catch (e: Exception) {
            Log.e("SubredditSearchSource", "Error", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, PostEntity>): String? {
        return null
    }
}
