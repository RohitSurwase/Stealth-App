package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.Award
import com.cosmos.unreddit.data.model.Block
import com.cosmos.unreddit.data.model.Flair
import com.cosmos.unreddit.data.model.PosterType
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.PostData
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.util.HtmlParser
import com.cosmos.unreddit.util.extension.formatNumber
import com.cosmos.unreddit.util.extension.toMillis
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

@Singleton
class PostMapper2 @Inject constructor(
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<PostData, PostEntity>(defaultDispatcher) {

    private val htmlParser: HtmlParser = HtmlParser()

    override suspend fun toEntity(from: PostData): PostEntity {
        with(from) {
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
                crossposts?.firstOrNull()?.let { toEntity(it) },
                selfTextHtml,
                Sorting(RedditApi.Sort.fromName(suggestedSort)),
                redditText,
                isOver18,
                previewUrl,
                (redditText.blocks.getOrNull(0)?.block as? Block.TextBlock)?.text,
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
                seen = false,
                saved = false
            )
        }
    }
}
