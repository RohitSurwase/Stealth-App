package com.cosmos.unreddit.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription")
data class Subscription (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    val name: String,

    @ColumnInfo(name = "time")
    val time: Long,

    @ColumnInfo(name = "icon")
    val icon: String?
)