package com.cosmos.unreddit.database

import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.api.pojo.details.Child
import com.cosmos.unreddit.api.pojo.details.ChildType
import com.cosmos.unreddit.api.pojo.details.PostChild
import com.cosmos.unreddit.api.pojo.list.PostData
import com.cosmos.unreddit.model.Flair
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.HtmlParser
import com.cosmos.unreddit.parser.TextBlock
import com.cosmos.unreddit.post.Award
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting
import com.cosmos.unreddit.util.formatNumber
import com.cosmos.unreddit.util.toMillis
import kotlin.math.round

object PostMapper {

    suspend fun dataToEntity(data: PostData, htmlParser: HtmlParser = HtmlParser()): PostEntity {
        with(data) {
            val redditText = htmlParser.separateHtmlBlocks(selfTextHtml)
            val flair = Flair.fromData(linkFlairRichText, flair)
            return PostEntity(
                name,
                prefixedSubreddit,
                title,
                round(ratio * 100).toInt(),
                totalAwards,
                isOC,
                flair,
                Flair.fromData(authorFlairRichText, authorFlair),
                isOver18 || isSpoiler || isOC || !flair.isEmpty() || isStickied || isArchived || isLocked,
                score.formatNumber(),
                postType,
                domain,
                isSelf,
                selfTextHtml,
                Sorting(RedditApi.Sort.fromName(suggestedSort)),
                redditText,
                isOver18,
                previewUrl,
                (redditText.blocks.getOrNull(0)?.block as? TextBlock)?.text,
                awardings.sortedByDescending { it.count }.map { Award(it.count, it.getIcon()) },
                isSpoiler,
                isArchived,
                isLocked,
                PosterType.fromDistinguished(distinguished),
                author,
                commentsNumber.formatNumber(),
                permalink,
                isStickied,
                url,
                created.toMillis(),
                mediaType,
                mediaUrl,
                gallery,
                false
            )
        }
    }

    suspend fun dataToEntities(data: List<Child>?): List<PostEntity> {
        val htmlParser = HtmlParser()

        return data?.filter {
            it.kind == ChildType.t3
        }?.map {
            dataToEntity((it as PostChild).data, htmlParser)
        } ?: emptyList()
    }
}
