package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.db.ProfileWithDetails
import com.cosmos.unreddit.data.model.backup.Profile
import com.cosmos.unreddit.data.model.backup.Subscription
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileMapper @Inject constructor(
    private val subscriptionMapper: SubscriptionMapper,
    private val backupPostMapper: BackupPostMapper,
    private val backupCommentMapper: BackupCommentMapper,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<ProfileWithDetails, Profile>(defaultDispatcher) {

    override suspend fun toEntity(from: ProfileWithDetails): Profile {
        return with(from) {
            Profile(
                profile.name,
                subscriptionMapper.dataToEntities(subscription),
                backupPostMapper.dataToEntities(savedPosts),
                backupCommentMapper.dataToEntities(savedComments)
            )
        }
    }
}
