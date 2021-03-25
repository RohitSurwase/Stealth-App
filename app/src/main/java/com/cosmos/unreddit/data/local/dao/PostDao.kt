package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import com.cosmos.unreddit.data.model.db.PostEntity

@Dao
abstract class PostDao : BaseDao<PostEntity> {
}