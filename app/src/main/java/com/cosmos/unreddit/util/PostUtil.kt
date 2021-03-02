package com.cosmos.unreddit.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.preferences.ContentPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object PostUtil {
    fun getProcessedUrl(url: String): String = url.replace("&amp;", "&")

    // TODO: Move somewhere else
    fun filterPosts(
        posts: Flow<PagingData<PostEntity>>,
        history: Flow<List<String>>,
        contentPreferences: Flow<ContentPreferences>
    ): Flow<PagingData<PostEntity>> {
        return combine(
            posts,
            history,
            contentPreferences
        ) { _posts, _history, _contentPreferences ->
            _posts.filter { post ->
                _contentPreferences.showNsfw || !post.isOver18
            }.map { post ->
                if (_history.contains(post.id)) {
                    post.seen = true
                }
                post
            }
        }
    }

    fun getAuthorGradientColor(
        context: Context,
        @ColorRes start: Int,
        @ColorRes end: Int
    ): IntArray {
        return intArrayOf(
            ContextCompat.getColor(context, start),
            ContextCompat.getColor(context, end)
        )
    }
}
