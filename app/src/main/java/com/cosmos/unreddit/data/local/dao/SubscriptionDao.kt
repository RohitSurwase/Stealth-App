package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubscriptionDao : BaseDao<Subscription> {

    @Query("DELETE FROM subscription WHERE name = :name")
    abstract suspend fun deleteFromName(name: String)

    @Query("SELECT * FROM subscription")
    abstract fun getSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT name FROM subscription WHERE profile_id = :profileId")
    abstract fun getSubscriptionsNamesFromProfile(profileId: Int): Flow<List<String>>
}
