package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.JsonMore
import com.cosmos.unreddit.data.remote.api.reddit.model.Data
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibBlockedException
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibChallenge
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibCommentScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibPostScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibSubredditScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibSubredditSearchScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibUserCommentScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibUserScraper
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.NetworkModule.RedlibBaseUrl
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redlib source: fetches the HTML pages served by a Redlib instance and parses
 * them with the Jsoup scrapers in `scraper.redlib`.
 */
@Singleton
class TedditSource @Inject constructor(
    private val tedditApi: TedditApi,
    @RedlibBaseUrl private val baseUrl: HttpUrl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseRedditSource {

    private val baseUrlString = baseUrl.toString()

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val normalizedSort = sort.toSubredditSort()
        // Record the exact listing URL so a bot-challenge failure can be
        // retried against the same path (Cloudflare challenges can be
        // path-specific) rather than just the instance root.
        val listingUrl = "${baseUrlString.trimEnd('/')}/r/$subreddit/${normalizedSort.type}"
        return try {
            val response = tedditApi.getSubreddit(
                subreddit,
                normalizedSort,
                timeSorting,
                after
            )
            RedlibPostScraper(ioDispatcher, baseUrlString).scrap(response.string())
        } catch (exception: RedlibBlockedException) {
            throw RedlibBlockedException(exception.message ?: "Instance blocked", listingUrl)
        } catch (exception: HttpException) {
            // Cloudflare/Anubis challenges are often served with a non-2xx
            // status (e.g. 403), so Retrofit throws before the scraper parses
            // the body. Inspect the error body for challenge markers and fail
            // loudly so the feed shows the inline-verification error instead of
            // a blank screen. Non-challenge errors are re-thrown unchanged.
            val body = (exception.response()?.body() as? okhttp3.ResponseBody)?.string().orEmpty()
            if (RedlibChallenge.isChallenge(exception.code(), body)) {
                throw RedlibBlockedException(
                    "Redlib instance returned an anti-bot challenge page instead of content",
                    listingUrl
                )
            }
            throw exception
        }
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        val response = tedditApi.getSubredditInfo(subreddit)
        return RedlibSubredditScraper(ioDispatcher, baseUrlString).scrap(response.string())
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = tedditApi.searchInSubreddit(
            subreddit,
            query,
            sort?.toSearchSort(),
            timeSorting,
            after = after
        )
        return RedlibPostScraper(ioDispatcher, baseUrlString).scrap(response.string())
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        val response = tedditApi.getPost(permalink, sort)
        val body = response.string()

        val post = RedlibPostScraper(ioDispatcher, baseUrlString).scrap(body)

        val subreddit = SUBREDDIT_REGEX.find(permalink)?.groupValues?.getOrNull(1).orEmpty()
        val postId = POST_ID_REGEX.find(permalink)?.groupValues?.getOrNull(1).orEmpty()

        val comments = RedlibCommentScraper(
            ioDispatcher,
            baseUrlString,
            "t3_$postId",
            subreddit
        ).scrap(body)

        return listOf(post, comments)
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        // Redlib does not expose Reddit's /api/morechildren endpoint
        return MoreChildren(JsonMore(Data(emptyList())))
    }

    override suspend fun getUserInfo(user: String): Child {
        val response = tedditApi.getUserInfo(user)
        return RedlibUserScraper(ioDispatcher, baseUrlString).scrap(response.string())
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = tedditApi.getUserPosts(
            user,
            sort.toUserSort(),
            timeSorting,
            after
        )
        return RedlibPostScraper(ioDispatcher, baseUrlString).scrap(response.string())
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = tedditApi.getUserComments(
            user,
            sort.toUserSort(),
            timeSorting,
            after
        )
        return RedlibUserCommentScraper(ioDispatcher, baseUrlString, user)
            .scrap(response.string())
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = tedditApi.searchPost(
            query,
            sort?.toSearchSort(),
            timeSorting,
            after
        )
        return RedlibPostScraper(ioDispatcher, baseUrlString).scrap(response.string())
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // Redlib does not support searching users
        return Listing("t2", ListingData(null, null, emptyList(), null, null))
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val response = tedditApi.searchSubreddit(
            query,
            sort?.toSearchSort(),
            timeSorting,
            after
        )
        return RedlibSubredditSearchScraper(ioDispatcher, baseUrlString)
            .scrap(response.string())
    }

    /**
     * Normalises a sort to one of the values accepted by Redlib's subreddit
     * route (hot/new/top/rising/controversial).
     */
    private fun Sort.toSubredditSort(): Sort {
        return when (this) {
            Sort.HOT, Sort.NEW, Sort.TOP, Sort.RISING, Sort.CONTROVERSIAL -> this
            else -> Sort.HOT
        }
    }

    /**
     * Normalises a sort to one of the values accepted by Redlib's user route
     * (hot/new/top/controversial).
     */
    private fun Sort.toUserSort(): Sort {
        return when (this) {
            Sort.HOT, Sort.NEW, Sort.TOP, Sort.CONTROVERSIAL -> this
            else -> Sort.HOT
        }
    }

    /**
     * Normalises a sort to one of the values accepted by Redlib's search
     * route (relevance/hot/top/new/comments).
     */
    private fun Sort.toSearchSort(): Sort {
        return when (this) {
            Sort.RELEVANCE, Sort.HOT, Sort.TOP, Sort.NEW, Sort.COMMENTS -> this
            else -> Sort.RELEVANCE
        }
    }

    companion object {
        private val SUBREDDIT_REGEX = Regex("/r/([^/]+)/comments/")
        private val POST_ID_REGEX = Regex("/comments/([^/?#]+)")
    }
}
