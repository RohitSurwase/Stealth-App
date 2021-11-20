package com.cosmos.unreddit.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.cosmos.unreddit.data.local.mapper.PostMapper2
import com.cosmos.unreddit.data.model.Data
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.reddit.model.PostData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

object PostUtil {
    fun getProcessedUrl(url: String): String = url.replace("&amp;", "&")

    suspend fun filterPosts(
        pagingData: PagingData<Child>,
        user: Data.User,
        postMapper: PostMapper2,
        defaultDispatcher: CoroutineDispatcher
    ): PagingData<PostEntity> = withContext(defaultDispatcher) {
        pagingData
            .map { postMapper.dataToEntity((it as PostChild).data) }
            .filter { post ->
                user.contentPreferences.showNsfw || !post.isOver18
            }
            .map { post ->
                post.apply {
                    seen = user.history.contains(post.id)
                    saved = user.saved.contains(post.id)
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
