package com.cosmos.unreddit.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.reddit.model.PostData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object PostUtil {
    fun getProcessedUrl(url: String): String = url.replace("&amp;", "&")

    // TODO: Move somewhere else
    fun filterPosts(
        posts: Flow<PagingData<PostEntity>>,
        history: Flow<List<String>>,
        saved: Flow<List<String>>,
        contentPreferences: Flow<ContentPreferences>
    ): Flow<PagingData<PostEntity>> {
        return combine(
            posts,
            history,
            saved,
            contentPreferences
        ) { _posts, _history, _saved, _contentPreferences ->
            _posts.filter { post ->
                _contentPreferences.showNsfw || !post.isOver18
            }.map { post ->
                post.apply {
                    this.seen = _history.contains(post.id)
                    this.saved = _saved.contains(post.id)
                }
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

    fun getPostData(listings: List<Listing>): PostData {
        return (listings[0].data.children[0] as PostChild).data
    }

    fun getCommentsData(listings: List<Listing>): List<Child> {
        return listings[1].data.children
    }
}
