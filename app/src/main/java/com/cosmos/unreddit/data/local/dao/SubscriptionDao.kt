package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubscriptionDao : BaseDao<Subscription> {

    @Query("DELETE FROM subscription WHERE name = :name AND profile_id = :profileId")
    abstract suspend fun deleteFromNameAndProfile(name: String, profileId: Int)

    @Query("DELETE FROM subscription WHERE profile_id = :profileId")
    abstract suspend fun deleteFromProfile(profileId: Int)

    @Query("SELECT * FROM subscription WHERE profile_id = :profileId")
    abstract fun getSubscriptionsFromProfile(profileId: Int): Flow<List<Subscription>>

    @Query("SELECT name FROM subscription WHERE profile_id = :profileId")
    abstract fun getSubscriptionsNamesFromProfile(profileId: Int): Flow<List<String>>
}
