package com.cosmos.unreddit.util

import androidx.paging.PagingData
import com.cosmos.unreddit.data.model.Sorting
import kotlinx.coroutines.flow.Flow

abstract class PagerHelper<T : Any> {

    private var currentQuery: String? = null
    private var currentSorting: Sorting? = null
    private var currentResults: Flow<PagingData<T>>? = null

    fun loadData(query: String, sorting: Sorting): Flow<PagingData<T>> {
        val lastResults = currentResults
        if (currentQuery == query &&
            currentSorting == sorting &&
            lastResults != null
        ) {
            return lastResults
        }

        currentQuery = query
        currentSorting = sorting

        val newResults = getResults(query, sorting)
        currentResults = newResults

        return newResults
    }

    protected abstract fun getResults(query: String, sorting: Sorting): Flow<PagingData<T>>
}
