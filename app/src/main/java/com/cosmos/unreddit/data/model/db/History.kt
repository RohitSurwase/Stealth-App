package com.cosmos.unreddit.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "post_id")
    val postId: String,

    @ColumnInfo(name = "time")
    val time: Long
)
