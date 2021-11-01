package com.cosmos.unreddit.data.remote.datasource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.local.mapper.PostMapper
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import retrofit2.HttpException
import java.io.IOException

open class PostListDataSource(
    private val redditApi: RedditApi,
    private val query: String,
    private val sorting: Sorting
) : PagingSource<String, PostEntity>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, PostEntity> {
        return try {
            val response = getResponse(query, sorting, params.key)
            val data = response.data

            val items = PostMapper.dataToEntities(data.children)

            LoadResult.Page(items, data.before, data.after)
        } catch (exception: IOException) {
            Log.e("PostListDataSource", "Error", exception)
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            Log.e("PostListDataSource", "Error", exception)
            LoadResult.Error(exception)
        }
    }

    open suspend fun getResponse(query: String, sorting: Sorting, after: String?): Listing {
        return redditApi.getSubreddit(query, sorting.generalSorting, sorting.timeSorting, after)
    }

    override fun getRefreshKey(state: PagingState<String, PostEntity>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
