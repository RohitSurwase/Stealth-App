package com.cosmos.unreddit.data.model.db

import androidx.room.Embedded
import androidx.room.Relation
import com.cosmos.unreddit.data.model.Comment

data class ProfileWithDetails(
    @Embedded
    val profile: Profile,

    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val subscription: List<Subscription>,

    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val savedPosts: List<PostEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val savedComments: List<Comment.CommentEntity>
)
