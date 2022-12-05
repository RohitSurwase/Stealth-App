package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cosmos.unreddit.data.model.db.Redirect
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RedirectDao : BaseDao<Redirect> {

    @Query("SELECT * FROM redirect")
    abstract fun getAllRedirects(): Flow<List<Redirect>>
}
