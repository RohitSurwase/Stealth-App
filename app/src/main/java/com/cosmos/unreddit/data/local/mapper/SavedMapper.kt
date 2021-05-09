package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.Block
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.SavedItem
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.util.HtmlParser

object SavedMapper {

    suspend fun dataToEntity(data: PostEntity, htmlParser: HtmlParser = HtmlParser()): SavedItem {
        val redditText = htmlParser.separateHtmlBlocks(data.selfTextHtml)
        return SavedItem.Post(
            data.apply {
                hasFlairs = isOver18 || isSpoiler || isOC || isStickied || isArchived || isLocked
                selfRedditText = redditText
                previewText = (redditText.blocks.firstOrNull()?.block as? Block.TextBlock)?.text
            }
        )
    }

    suspend fun dataToEntity(
        data: Comment.CommentEntity,
        htmlParser: HtmlParser = HtmlParser()
    ): SavedItem {
        return SavedItem.Comment(
            data.apply {
                body = htmlParser.separateHtmlBlocks(bodyHtml)
            }
        )
    }

    suspend fun postsToEntities(data: List<PostEntity>): List<SavedItem> {
        val htmlParser = HtmlParser()

        return data.map { dataToEntity(it, htmlParser) }
    }

    suspend fun commentsToEntities(data: List<Comment.CommentEntity>): List<SavedItem> {
        val htmlParser = HtmlParser()

        return data.map { dataToEntity(it, htmlParser) }
    }
}
