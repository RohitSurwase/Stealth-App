package com.cosmos.unreddit.data.remote.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import com.cosmos.unreddit.util.RedditUtil
import com.cosmos.unreddit.util.extension.interlace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.ceil

class SmartPostListDataSource(
    private val source: CurrentSource,
    private val query: List<String>,
    private val sorting: Sorting,
    private val defaultDispatcher: CoroutineDispatcher,
    mainImmediateDispatcher: CoroutineDispatcher
) : PagingSource<List<String>, Child>() {

    private val scope = CoroutineScope(mainImmediateDispatcher + SupervisorJob())

    private val joinedQuery by lazy { RedditUtil.joinSubredditList(query) }
    private val chunkSize by lazy {
        // Find the optimal chunk size to have lists of similar sizes
        ceil(query.size / ceil(query.size / REDDIT_SUBREDDIT_LIMIT.toDouble())).toInt()
    }

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<List<String>>): LoadResult<List<String>, Child> {
        return try {
            if (query.size > REDDIT_SUBREDDIT_LIMIT) getSmartData(params) else getData(params)
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<List<String>, Child>): List<String>? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    private suspend fun getSmartData(
        params: LoadParams<List<String>>
    ): LoadResult<List<String>, Child> {
        val queries = withContext(defaultDispatcher) {
            query
                // Step 1: Split the subreddit list into chunks
                .chunked(chunkSize)
                // Step 2: Create the query string for each chunk
                .map { RedditUtil.joinSubredditList(it) }
                // Step 3: Map each chunk with its `after` key (if available)
                .mapIndexed { index, chunkedList -> chunkedList to params.key?.getOrNull(index) }
        }

        // Step 4: Request the posts for each chunk in parallel
        val responses = queries
            .map {
                scope.async {
                    source.getSubreddit(
                        it.first,
                        sorting.generalSorting,
                        sorting.timeSorting,
                        it.second
                    )
                }
            }
            .awaitAll()

        // Step 5: Flatten (and sort) the responses in order to have a single list of posts
        val data = withContext(defaultDispatcher) {
            responses
                .map { it.data.children }
                .sort(sorting)
                .distinctBy { (it as PostChild).data.name }
        }

        // Step 6: Retrieve the `after` key for each response and create a list out of them
        val after = withContext(defaultDispatcher) {
            responses.map { it.data.after ?: "" }
        }

        return LoadResult.Page(data, null, after)
    }

    private suspend fun getData(params: LoadParams<List<String>>): LoadResult<List<String>, Child> {
        val response = source.getSubreddit(
            joinedQuery,
            sorting.generalSorting,
            sorting.timeSorting,
            params.key?.getOrNull(0)
        )

        val data = response.data

        return LoadResult.Page(data.children, null, data.after?.let { listOf(it) })
    }

    private fun List<List<Child>>.sort(sorting: Sorting): List<Child> {
        return when(sorting.generalSorting) {
            // If sorting is set to NEW, simply flatten the lists and sort the posts by date
            Sort.NEW -> this.flatten().sortedByDescending { (it as PostChild).data.created }
            // If sorting is set to TOP, simply flatten the lists and sort the posts by score
            Sort.TOP -> this.flatten().sortedByDescending { (it as PostChild).data.score }
            // For all the other sorting methods, interlace the lists to have a consistent result
            // [['a', 'b', 'c'], ['e', 'f', 'g'], ['h', 'i']] ==> ['a', 'e', 'h', 'b', 'f', 'i', 'c', 'g']
            else -> this.interlace()
        }
    }

    companion object {
        private const val REDDIT_SUBREDDIT_LIMIT = 100
    }
}
