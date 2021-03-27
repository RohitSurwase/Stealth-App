package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.Award
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Comment.MoreEntity
import com.cosmos.unreddit.data.model.Flair
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.ChildType
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentData
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChild
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreData
import com.cosmos.unreddit.util.HtmlParser
import com.cosmos.unreddit.util.extension.toMillis

object CommentMapper {

    suspend fun dataToEntity(
        data: CommentData,
        htmlParser: HtmlParser = HtmlParser()
    ): CommentEntity {
        with(data) {
            return CommentEntity(
                totalAwards,
                linkId,
                dataToEntities(replies?.data?.children),
                author,
                scoreString,
                awardings.sortedByDescending { it.count }.map { Award(it.count, it.getIcon()) },
                htmlParser.separateHtmlBlocks(bodyHtml),
                editedMillis,
                isSubmitter,
                stickied,
                scoreHidden,
                permalink,
                id,
                created.toMillis(),
                controversiality,
                Flair.fromData(authorFlairRichText, flair),
                PosterType.fromDistinguished(distinguished),
                linkTitle,
                linkPermalink,
                linkAuthor,
                subreddit,
                commentIndicator,
                name,
                depth ?: 0
            )
        }
    }

    fun dataToEntity(data: MoreData): MoreEntity {
        with(data) {
            return MoreEntity(
                count,
                children,
                id,
                parentId,
                name,
                depth ?: 0
            )
        }
    }

    suspend fun dataToEntities(data: List<Child>?): MutableList<Comment> {
        val htmlParser = HtmlParser()

        return data?.mapNotNull {
            when (it.kind) {
                ChildType.t1 -> dataToEntity((it as CommentChild).data, htmlParser)
                ChildType.more -> dataToEntity((it as MoreChild).data)
                else -> null
            }
        } as MutableList<Comment>? ?: mutableListOf()
    }
}
