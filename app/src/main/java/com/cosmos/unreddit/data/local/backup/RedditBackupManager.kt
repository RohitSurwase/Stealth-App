package com.cosmos.unreddit.data.local.backup

import android.content.Context
import android.net.Uri
import com.cosmos.unreddit.data.local.RedditDatabase
import com.cosmos.unreddit.data.local.mapper.BackupCommentMapper
import com.cosmos.unreddit.data.local.mapper.BackupPostMapper
import com.cosmos.unreddit.data.local.mapper.ProfileMapper
import com.cosmos.unreddit.data.local.mapper.SubredditMapper2
import com.cosmos.unreddit.data.local.mapper.SubscriptionMapper
import com.cosmos.unreddit.data.model.backup.Profile
import com.cosmos.unreddit.data.model.backup.Subscription
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.NetworkModule.RedditMoshi
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditBackupManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    redditDatabase: RedditDatabase,
    profileMapper: ProfileMapper,
    subscriptionMapper: SubscriptionMapper,
    backupPostMapper: BackupPostMapper,
    backupCommentMapper: BackupCommentMapper,
    private val subredditMapper: SubredditMapper2,
    @RedditMoshi private val moshi: Moshi,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupManager(
    redditDatabase,
    profileMapper,
    subscriptionMapper,
    backupPostMapper,
    backupCommentMapper,
    defaultDispatcher
) {

    override suspend fun import(uri: Uri): Result<List<Profile>> {
        val adapter = moshi.adapter(Listing::class.java)
        val profiles = mutableListOf<Profile>()

        return runCatching {
            withContext(ioDispatcher) {
                appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    adapter.fromJson(inputStream.source().buffer())
                }
            }
        }.onSuccess {
            it?.let { listing ->
                val subscriptions = mapSubredditsToSubscriptions(listing)
                profiles.add(Profile("Reddit", subscriptions))

                insertProfiles(profiles)
            }
        }.map {
            profiles
        }
    }

    private suspend fun mapSubredditsToSubscriptions(listing: Listing): List<Subscription> {
        return withContext(defaultDispatcher) {
            listing.data.children
                .map {
                    subredditMapper.dataToEntity((it as AboutChild).data)
                }
                .map {
                    Subscription(it.displayName, System.currentTimeMillis(), it.icon)
                }
        }
    }

    override suspend fun export(uri: Uri): Result<List<Profile>> {
        throw UnsupportedOperationException("Cannot export profiles to Reddit format")
    }
}
