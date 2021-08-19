package com.cosmos.unreddit.util

object SearchUtil {
    private const val QUERY_MIN_LENGTH = 3
    private const val QUERY_MAX_LENGTH = 20

    fun isQueryValid(query: String): Boolean {
        return query.length in QUERY_MIN_LENGTH..QUERY_MAX_LENGTH
    }
}
