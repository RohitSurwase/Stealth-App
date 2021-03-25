package com.cosmos.unreddit.data.remote.datasource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.local.mapper.CommentMapper
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi

class CommentsDataSource(
    private val redditApi: RedditApi,
    private val user: String,
    private val sorting: Sorting
) : PagingSource<String, Comment>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val response = redditApi.getUserComments(
                user,
                sorting.generalSorting,
                sorting.timeSorting, params.key
            )
            val data = response.data

            val items = CommentMapper.dataToEntities(data.children)

            LoadResult.Page(items, data.before, data.after)
        } catch (e: Exception) {
            Log.e("CommentsDataSource", "Error", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Comment>): String? {
        return null
    }
}
