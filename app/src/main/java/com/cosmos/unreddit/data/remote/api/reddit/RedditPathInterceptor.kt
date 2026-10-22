package com.cosmos.unreddit.data.remote.api.reddit

import okhttp3.Interceptor
import okhttp3.Response

class RedditPathInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val encodedPath = encodePath(path)

        if (encodedPath == path) {
            return chain.proceed(request)
        }

        val newUrl = request.url.newBuilder()
            .encodedPath(encodedPath)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }

    private fun encodePath(path: String): String {
        val encodedPath = encodeSubredditList(path)

        if (encodedPath.startsWith("/r/")) {
            val commentsIndex = encodedPath.indexOf(COMMENTS, startIndex = 3)
            if (commentsIndex >= 0) {
                val tail = encodedPath.substring(commentsIndex + COMMENTS.length)
                if (tail.isEmpty() || tail.startsWith("/") || tail.startsWith(".")) {
                    return encodedPath.substring(0, commentsIndex) + "%2fcomments" + tail
                }
            }
        }

        return when {
            encodedPath.startsWith("/api/morechildren") ->
                "/api%2fmorechildren" + encodedPath.removePrefix("/api/morechildren")
            encodedPath.startsWith("/api/info") ->
                "/api%2finfo" + encodedPath.removePrefix("/api/info")
            else -> encodedPath
        }
    }

    // Reddit blocks literal "+" multi-subreddit paths for app-only OAuth tokens with a
    // 301 to the homepage. Encoding the separator as "%2B" is accepted, just like the
    // "%2f" workaround used for comment routes.
    private fun encodeSubredditList(path: String): String {
        if (!path.startsWith("/r/")) {
            return path
        }

        val endIndex = path.indexOf('/', startIndex = 3).let {
            if (it == -1) path.length else it
        }
        val subreddits = path.substring(3, endIndex)

        if ('+' !in subreddits) {
            return path
        }

        return path.substring(0, 3) +
            subreddits.replace("+", "%2B") +
            path.substring(endIndex)
    }

    companion object {
        private const val COMMENTS = "/comments"
    }
}
