package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.Profile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao : BaseDao<Profile> {
    @Query("SELECT * FROM profile")
    abstract fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profile WHERE id = :id")
    abstract suspend fun getProfileFromId(id: Int): Profile?

    @Query("SELECT * FROM profile LIMIT 1")
    abstract suspend fun getFirstProfile(): Profile

    @Query("DELETE FROM profile  WHERE id = :id")
    abstract suspend fun deleteFromId(id: Int)
}
