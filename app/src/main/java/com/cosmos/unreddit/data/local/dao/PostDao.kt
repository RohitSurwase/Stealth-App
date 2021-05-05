package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PostDao : BaseDao<PostEntity> {

    @Query("DELETE FROM post WHERE id = :id AND profile_id = :profileId")
    abstract suspend fun deleteFromIdAndProfile(id: String, profileId: Int)

    @Query("SELECT id FROM post WHERE profile_id = :profileId")
    abstract fun getSavedPostIdsFromProfile(profileId: Int): Flow<List<String>>

    @Query("SELECT * FROM post WHERE profile_id = :profileId")
    abstract fun getSavedPostsFromProfile(profileId: Int): Flow<List<PostEntity>>
}
