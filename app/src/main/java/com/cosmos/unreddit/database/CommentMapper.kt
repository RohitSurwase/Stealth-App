package com.cosmos.unreddit.database

import com.cosmos.unreddit.api.pojo.details.*
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.HtmlParser
import com.cosmos.unreddit.post.Award
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.post.MoreEntity

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
                score,
                awardings.sortedByDescending { it.count }.map { Award(it.count, it.getIcon()) },
                htmlParser.separateHtmlBlocks(bodyHtml),
                getEditedTimeInMillis(),
                isSubmitter,
                stickied,
                scoreHidden,
                permalink,
                id,
                getTimeInMillis(),
                controversiality,
                Flair.fromData(authorFlairRichText, flair),
                PosterType.fromDistinguished(distinguished),
                linkTitle,
                linkPermalink,
                linkAuthor,
                subreddit,
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
        val commentList = mutableListOf<Comment>()

        val htmlParser = HtmlParser()

        data?.forEach {
            commentList.add(
                when (it.kind) {
                    ChildType.t1 -> dataToEntity((it as CommentChild).data, htmlParser)
                    ChildType.more -> dataToEntity((it as MoreChild).data)
                    else -> return@forEach
                }
            )
        }

        return commentList
    }
}
