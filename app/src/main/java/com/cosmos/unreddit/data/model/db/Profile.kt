package com.cosmos.unreddit.data.model.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    var canDelete: Boolean = false
}
