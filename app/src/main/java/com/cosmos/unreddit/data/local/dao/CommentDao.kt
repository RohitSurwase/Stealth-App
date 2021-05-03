package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.Comment
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CommentDao : BaseDao<Comment.CommentEntity> {

    @Query("DELETE FROM comment WHERE name = :name AND profile_id = :profileId")
    abstract suspend fun deleteFromIdAndProfile(name: String, profileId: Int)

    @Query("SELECT name FROM comment WHERE profile_id = :profileId")
    abstract fun getSavedCommentsFromProfile(profileId: Int): Flow<List<String>>
}
