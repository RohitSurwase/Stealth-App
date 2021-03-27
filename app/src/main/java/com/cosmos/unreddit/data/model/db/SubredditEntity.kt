package com.cosmos.unreddit.data.model.db

import com.cosmos.unreddit.data.model.RedditText
import kotlin.math.round

data class SubredditEntity(
    val wikiEnabled: Boolean,

    val displayName: String,

    val header: String?,

    val title: String,

    val primaryColor: Int,

    val activeUserCount: Int?,

    val icon: String?,

    val subscribers: Int?,

    val quarantine: Boolean,

    val publicDescription: RedditText,

    val keyColor: Int,

    val backgroundColor: Int,

    val over18: Boolean,

    val description: RedditText,

    val url: String,

    val created: Long
) {
    fun getSubscribersCount(): String {
        return when {
            subscribers == null -> "" // TODO
            subscribers < 1000 -> subscribers.toString()
            subscribers < 1_000_000 -> {
                val roundedSubscribers = round(subscribers.div(1000f)).toInt()
                "${roundedSubscribers}k"
            }
            else -> {
                val roundedSubscribers = String.format("%.1f", subscribers.div(1_000_000f))
                "${roundedSubscribers}m"
            }
        }
    }

    fun getActiveUsers(): String {
        return when {
            activeUserCount == null -> "0" // TODO
            activeUserCount < 1000 -> activeUserCount.toString()
            else -> {
                val roundedUsers = String.format("%.1f", activeUserCount.div(1000f))
                "${roundedUsers}k"
            }
        }
    }
}
