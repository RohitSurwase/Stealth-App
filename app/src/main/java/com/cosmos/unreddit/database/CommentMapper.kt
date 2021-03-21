package com.cosmos.unreddit.database

import com.cosmos.unreddit.api.pojo.details.Child
import com.cosmos.unreddit.api.pojo.details.ChildType
import com.cosmos.unreddit.api.pojo.details.CommentChild
import com.cosmos.unreddit.api.pojo.details.CommentData
import com.cosmos.unreddit.api.pojo.details.MoreChild
import com.cosmos.unreddit.api.pojo.details.MoreData
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.HtmlParser
import com.cosmos.unreddit.post.Award
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.post.MoreEntity
import com.cosmos.unreddit.util.toMillis

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
