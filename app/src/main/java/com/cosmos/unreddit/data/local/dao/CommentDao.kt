package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CommentDao : BaseDao<CommentEntity> {

    @Query("DELETE FROM comment WHERE name = :name AND profile_id = :profileId")
    abstract suspend fun deleteFromIdAndProfile(name: String, profileId: Int)

    @Query("DELETE FROM comment WHERE profile_id = :profileId")
    abstract suspend fun deleteFromProfile(profileId: Int)

    @Query("SELECT name FROM comment WHERE profile_id = :profileId")
    abstract fun getSavedCommentIdsFromProfile(profileId: Int): Flow<List<String>>

    @Query("SELECT * FROM comment WHERE profile_id = :profileId")
    abstract fun getSavedCommentsFromProfile(profileId: Int): Flow<List<CommentEntity>>
}
