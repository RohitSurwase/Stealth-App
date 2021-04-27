package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.Profile

@Dao
abstract class ProfileDao : BaseDao<Profile> {
    @Query("SELECT * FROM profile WHERE id = :id")
    abstract suspend fun getProfileFromId(id: Int): Profile?

    @Query("SELECT * FROM profile LIMIT 1")
    abstract suspend fun getFirstProfile(): Profile
}
