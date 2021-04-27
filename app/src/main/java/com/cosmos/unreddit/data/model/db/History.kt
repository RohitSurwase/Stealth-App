package com.cosmos.unreddit.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "history",
    primaryKeys = ["post_id", "profile_id"],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class History(
    @ColumnInfo(name = "post_id")
    val postId: String,

    @ColumnInfo(name = "time")
    val time: Long,

    @ColumnInfo(name = "profile_id")
    val profileId: Int
)
