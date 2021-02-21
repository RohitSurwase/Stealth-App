package com.cosmos.unreddit.database

import com.cosmos.unreddit.api.RedditApi
import com.cosmos.unreddit.api.pojo.details.Child
import com.cosmos.unreddit.api.pojo.details.ChildType
import com.cosmos.unreddit.api.pojo.details.PostChild
import com.cosmos.unreddit.api.pojo.list.PostData
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.HtmlParser
import com.cosmos.unreddit.post.Award
import com.cosmos.unreddit.post.PostEntity
import com.cosmos.unreddit.post.Sorting

object PostMapper {

    suspend fun dataToEntity(data: PostData, htmlParser: HtmlParser = HtmlParser()): PostEntity {
        with(data) {
            return PostEntity(
                name,
                subreddit,
                title,
                ratio,
                totalAwards,
                isOC,
                getFlair(), // TODO
                score,
                getPostType(),
                domain,
                isSelf,
                selfText,
                selfTextHtml,
                Sorting(RedditApi.Sort.fromName(suggestedSort)),
                htmlParser.separateHtmlBlocks(selfTextHtml),
                isPinned,
                isOver18,
                getPreviewUrl(),
                awardings.map { Award(it.count, it.getIcon()) },
                isSpoiler,
                isArchived,
                isLocked,
                PosterType.fromDistinguished(distinguished),
                author,
                commentsNumber,
                permalink,
                isStickied,
                url,
                getTimeInMillis(),
                false
            )
        }
    }

    suspend fun dataToEntities(data: List<Child>?): List<PostEntity> {
        val postList = mutableListOf<PostEntity>()

        val htmlParser = HtmlParser()

        data?.forEach {
            if (it.kind == ChildType.t3) {
                postList.add(dataToEntity((it as PostChild).data, htmlParser))
            }
        }

        return postList
    }
}
