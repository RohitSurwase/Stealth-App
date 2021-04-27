package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.History
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HistoryDao : BaseDao<History> {

    @Query("SELECT * FROM history")
    abstract fun getHistory(): Flow<List<History>>

    @Query("SELECT post_id FROM history WHERE profile_id = :profileId")
    abstract fun getHistoryIdsFromProfile(profileId: Int): Flow<List<String>>
}
