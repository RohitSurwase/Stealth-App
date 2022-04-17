package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.backup.Post
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.di.DispatchersModule
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupPostMapper @Inject constructor(
    @DispatchersModule.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<PostEntity, Post>(defaultDispatcher) {

    override suspend fun toEntity(from: PostEntity): Post {
        return with(from) {
            Post(
                id,
                subreddit,
                title,
                ratio,
                totalAwards,
                isOC,
                score,
                type,
                domain,
                isSelf,
                selfTextHtml,
                suggestedSorting,
                isOver18,
                preview,
                isSpoiler,
                isArchived,
                isLocked,
                posterType,
                author,
                commentsNumber,
                permalink,
                isStickied,
                url,
                created,
                mediaType,
                mediaUrl,
                time
            )
        }
    }

    override suspend fun fromEntity(from: Post): PostEntity {
        return with(from) {
            PostEntity(
                id,
                subreddit,
                title,
                ratio,
                totalAwards,
                isOC,
                score = score,
                type = type,
                domain = domain,
                isSelf = isSelf,
                selfTextHtml = selfTextHtml,
                suggestedSorting = suggestedSorting,
                isOver18 = isOver18,
                preview = preview,
                isSpoiler = isSpoiler,
                isArchived = isArchived,
                isLocked = isLocked,
                posterType = posterType,
                author = author,
                commentsNumber = commentsNumber,
                permalink = permalink,
                isStickied = isStickied,
                url = url,
                created = created,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                time = time
            )
        }
    }
}
