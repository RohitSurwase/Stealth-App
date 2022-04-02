package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.local.backup.SubscriptionBackup
import com.cosmos.unreddit.data.model.db.Subscription
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

typealias SubscriptionBackup = com.cosmos.unreddit.data.model.backup.Subscription

@Singleton
class SubscriptionMapper @Inject constructor(
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<Subscription, SubscriptionBackup>(defaultDispatcher) {

    override suspend fun toEntity(from: Subscription): SubscriptionBackup {
        return with(from) {
            SubscriptionBackup(name, time, icon)
        }
    }

    override suspend fun fromEntity(from: SubscriptionBackup): Subscription {
        return with(from) {
            Subscription(name, time, icon)
        }
    }
}
