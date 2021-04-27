package com.cosmos.unreddit.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "subscription",
    primaryKeys = ["name", "profile_id"],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subscription (
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    val name: String,

    @ColumnInfo(name = "time")
    val time: Long,

    @ColumnInfo(name = "icon")
    val icon: String?,

    @ColumnInfo(name = "profile_id")
    val profileId: Int
)
