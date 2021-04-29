package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import com.cosmos.unreddit.data.model.Comment

@Dao
abstract class CommentDao : BaseDao<Comment.CommentEntity> {

}
