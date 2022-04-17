package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.db.ProfileWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao : BaseDao<Profile> {
    @Query("SELECT * FROM profile")
    abstract fun getAllProfiles(): Flow<List<Profile>>

    @Transaction
    @Query("SELECT * FROM profile")
    abstract suspend fun getProfilesWithDetails(): List<ProfileWithDetails>

    @Query("SELECT * FROM profile WHERE id = :id")
    abstract suspend fun getProfileFromId(id: Int): Profile?

    @Query("SELECT * FROM profile LIMIT 1")
    abstract suspend fun getFirstProfile(): Profile

    @Query("DELETE FROM profile  WHERE id = :id")
    abstract suspend fun deleteFromId(id: Int)
}
