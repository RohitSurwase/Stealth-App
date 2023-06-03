package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Data
import com.cosmos.unreddit.data.remote.api.reddit.model.JsonMore
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.remote.api.reddit.scraper.CommentScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.Over18Scraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.PostScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.RedditScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.SubredditScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.SubredditSearchScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.UserScaper
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.DispatchersModule.MainImmediateDispatcher
import com.cosmos.unreddit.di.NetworkModule.RedditScrap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.FormBody
import okhttp3.ResponseBody
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditScrapingSource @Inject constructor(
    @RedditScrap private val redditApi: RedditApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher
) : BaseRedditSource {

    private val scope = CoroutineScope(mainImmediateDispatcher + SupervisorJob())

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return consentOver18(PostScraper(ioDispatcher)) {
            redditApi.getSubreddit(subreddit, sort, timeSorting, after)
        }
    }

    override suspend fun getSubredditInfo(subreddit: String): Child {
        return consentOver18(SubredditScraper(ioDispatcher)) {
            redditApi.getSubreddit(subreddit, Sort.HOT, null)
        }
    }

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO
        return Listing("t3", ListingData(null, null, emptyList(), null, null))
    }

    override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> {
        return consentOver18(redditApi.getPost(permalink, limit, sort)) { document, body ->
            val post = scope.async {
                PostScraper(ioDispatcher).scrap(document, body)
            }

            val comments = scope.async {
                CommentScraper(ioDispatcher).scrap(document, body)
            }

            listOf(post.await(), comments.await())
        }
    }

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        // TODO
        return MoreChildren(JsonMore(Data(emptyList())))
    }

    override suspend fun getUserInfo(user: String): Child {
        return consentOver18(UserScaper(ioDispatcher)) {
            redditApi.getUserPosts(user, Sort.HOT, null)
        }
    }

    override suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return consentOver18(PostScraper(ioDispatcher)) {
            redditApi.getUserPosts(user, sort, timeSorting, after)
        }
    }

    override suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return consentOver18(CommentScraper(ioDispatcher)) {
            redditApi.getUserComments(user, sort, timeSorting, after)
        }
    }

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO
        return Listing("t3", ListingData(null, null, emptyList(), null, null))
    }

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        // TODO
        return Listing("t2", ListingData(null, null, emptyList(), null, null))
    }

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        return consentOver18(SubredditSearchScraper(ioDispatcher)) {
            redditApi.searchSubreddit(query, sort, timeSorting, after)
        }
    }

    private suspend fun <T> consentOver18(
        response: ResponseBody,
        result: suspend (Document?, String?) -> T
    ): T {
        val over18Scraper = Over18Scraper(ioDispatcher)

        val dest = over18Scraper.scrap(response.string())

        if (dest == null) {
            val document = over18Scraper.document ?: error("Document cannot be null")
            return result.invoke(document, null)
        }

        val requestBody = FormBody.Builder()
            .add("over18", "yes")
            .build()

        val consentResponse = redditApi.consentOver18(requestBody, dest)

        return result.invoke(null, consentResponse.string())
    }

    private suspend fun <T> consentOver18(
        scraper: RedditScraper<T>,
        request: suspend () -> ResponseBody
    ): T {
        return consentOver18(request.invoke()) { document, body ->
            scraper.scrap(document, body)
        }
    }
}
