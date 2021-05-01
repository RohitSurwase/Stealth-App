package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.PostEntity

@Dao
abstract class PostDao : BaseDao<PostEntity> {

    @Query("DELETE FROM post WHERE id = :id AND profile_id = :profileId")
    abstract suspend fun deleteFromIdAndProfile(id: String, profileId: Int)
}
