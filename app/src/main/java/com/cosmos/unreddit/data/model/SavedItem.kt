package com.cosmos.unreddit.data.model

import com.cosmos.unreddit.data.model.db.PostEntity

sealed class SavedItem(val timestamp: Long) {
    data class Post(val post: PostEntity) : SavedItem(post.time)

    data class Comment(
        val comment: com.cosmos.unreddit.data.model.Comment.CommentEntity
    ) : SavedItem(comment.time)
}
