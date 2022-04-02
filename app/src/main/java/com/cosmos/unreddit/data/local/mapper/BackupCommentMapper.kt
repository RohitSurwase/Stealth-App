package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.di.DispatchersModule
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

typealias CommentBackup = com.cosmos.unreddit.data.model.backup.Comment

@Singleton
class BackupCommentMapper @Inject constructor(
    @DispatchersModule.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<CommentEntity, CommentBackup>(defaultDispatcher) {

    override suspend fun toEntity(from: CommentEntity): CommentBackup {
        return with(from) {
            CommentBackup(
                totalAwards,
                linkId,
                author,
                score,
                bodyHtml,
                edited,
                isSubmitter,
                stickied,
                scoreHidden,
                permalink,
                id,
                created,
                controversiality,
                posterType,
                linkTitle,
                linkPermalink,
                linkAuthor,
                subreddit,
                name,
                time
            )
        }
    }

    override suspend fun fromEntity(from: CommentBackup): CommentEntity {
        return with(from) {
            CommentEntity(
                totalAwards,
                linkId,
                author = author,
                score = score,
                bodyHtml = bodyHtml,
                edited = edited,
                isSubmitter = isSubmitter,
                stickied = stickied,
                scoreHidden = scoreHidden,
                permalink = permalink,
                id = id,
                created = created,
                controversiality = controversiality,
                posterType = posterType,
                linkTitle = linkTitle,
                linkPermalink = linkPermalink,
                linkAuthor = linkAuthor,
                subreddit = subreddit,
                name = name,
                time = time
            )
        }
    }
}
