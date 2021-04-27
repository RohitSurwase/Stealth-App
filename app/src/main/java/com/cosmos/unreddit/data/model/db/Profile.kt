package com.cosmos.unreddit.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String
)
