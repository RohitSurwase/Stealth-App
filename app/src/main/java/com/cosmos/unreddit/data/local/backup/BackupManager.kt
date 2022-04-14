package com.cosmos.unreddit.data.local.backup

import android.net.Uri
import com.cosmos.unreddit.data.local.RedditDatabase
import com.cosmos.unreddit.data.local.mapper.BackupCommentMapper
import com.cosmos.unreddit.data.local.mapper.BackupPostMapper
import com.cosmos.unreddit.data.local.mapper.ProfileMapper
import com.cosmos.unreddit.data.local.mapper.SubscriptionMapper
import com.cosmos.unreddit.data.model.backup.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

typealias SubscriptionBackup = com.cosmos.unreddit.data.model.backup.Subscription

sealed class BackupManager(
    protected val redditDatabase: RedditDatabase,
    private val profileMapper: ProfileMapper,
    private val subscriptionMapper: SubscriptionMapper,
    private val backupPostMapper: BackupPostMapper,
    private val backupCommentMapper: BackupCommentMapper,
    protected val defaultDispatcher: CoroutineDispatcher
) {

    protected suspend fun getProfiles(): List<Profile> {
        val profilesWithSubscriptions = redditDatabase.profileDao().getProfilesWithDetails()

        return profileMapper.dataToEntities(profilesWithSubscriptions)
    }

    protected suspend fun insertProfiles(profiles: List<Profile>) {
        withContext(defaultDispatcher) {
            insert(profiles)
        }
    }

    private suspend fun insert(profiles: List<Profile>) {
        val profileDao = redditDatabase.profileDao()
        val subscriptionDao = redditDatabase.subscriptionDao()
        val postDao = redditDatabase.postDao()
        val commentDao = redditDatabase.commentDao()

        profiles.forEach { profile ->
            val row = profileDao.insert(ProfileDb(name = profile.name))

            val insertedProfile = profileDao.getProfileFromId(row.toInt())

            insertedProfile?.id?.let { id ->
                subscriptionMapper
                    .dataFromEntities(profile.subscriptions)
                    .map { subscription -> subscription.apply { profileId = id } }
                    .toTypedArray()
                    .let { subscriptions ->
                        subscriptionDao.insert(*subscriptions)
                    }

                profile.savedPosts?.let { savedPosts ->
                    backupPostMapper
                        .dataFromEntities(savedPosts)
                        .map { post -> post.apply { profileId = id } }
                        .toTypedArray()
                        .let { posts ->
                            postDao.insert(*posts)
                        }
                }

                profile.savedComments?.let { savedComments ->
                    backupCommentMapper
                        .dataFromEntities(savedComments)
                        .map { comment -> comment.apply { profileId = id } }
                        .toTypedArray()
                        .let { comments ->
                            commentDao.insert(*comments)
                        }
                }
            }
        }
    }

    abstract suspend fun import(uri: Uri): Result<List<Profile>>

    abstract suspend fun export(uri: Uri): Result<List<Profile>>
}
