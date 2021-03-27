package com.cosmos.unreddit.util

import android.content.UriMatcher
import android.net.Uri

object RedditUri {

    enum class UriType(val code: Int) {
        SUBREDDIT(1), USER(2)
    }

    private const val SCHEME = "content"
    private const val AUTHORITY = "reddit"

    private const val SUBREDDIT_PATH = "subreddit"
    private const val USER_PATH = "user"

    private val BASE_URI = Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .build()

    val SUBREDDIT_URI: Uri = BASE_URI
        .buildUpon()
        .appendPath(SUBREDDIT_PATH)
        .build()

    fun getSubredditUri(subreddit: String): Uri {
        return SUBREDDIT_URI.buildUpon().appendPath(subreddit).build()
    }

    val USER_URI: Uri = BASE_URI
        .buildUpon()
        .appendPath(USER_PATH)
        .build()

    fun getUserUri(user: String): Uri {
        return USER_URI.buildUpon().appendPath(user).build()
    }

    private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

    init {
        URI_MATCHER.addURI(AUTHORITY, "$SUBREDDIT_PATH/*", UriType.SUBREDDIT.code)
        URI_MATCHER.addURI(AUTHORITY, "$USER_PATH/*", UriType.USER.code)
    }

    fun getUriType(uri: Uri?): UriType? {
        if (uri == null) return null

        return when (URI_MATCHER.match(uri)) {
            UriType.SUBREDDIT.code -> UriType.SUBREDDIT
            UriType.USER.code -> UriType.USER
            else -> null
        }
    }
}