package com.cosmos.unreddit

import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentData
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.PostChild
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibCommentScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibPostScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibSubredditScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibSubredditSearchScraper
import com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib.RedlibUserCommentScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedlibScraperTest {

    private val baseUrl = "https://redlib.example/"

    @Test
    fun `post listing is parsed`() = runBlocking {
        val listing = RedlibPostScraper(Dispatchers.Unconfined, baseUrl)
            .scrap(LISTING)

        assertEquals("t3", listing.kind)
        assertEquals("t3_next", listing.data.after)
        assertEquals(4, listing.data.children.size)

        val selfPost = (listing.data.children[1] as PostChild).data
        assertTrue(selfPost.isSelf)
        assertEquals("pics", selfPost.subreddit)
        assertEquals("r/pics", selfPost.prefixedSubreddit)
        assertEquals("t3_self1", selfPost.name)
        assertEquals("https://redlib.example/r/pics/comments/self1/self_title/", selfPost.url)

        val linkPost = (listing.data.children[2] as PostChild).data
        assertEquals("https://example.com/story", linkPost.url)
        assertEquals("example.com", linkPost.domain)

        val galleryPost = (listing.data.children[3] as PostChild).data
        assertEquals(2, galleryPost.gallery.size)
        assertEquals(listOf("image", "video"), galleryPost.gallery.map { it.type.name.lowercase() })
    }

    @Test
    fun `post detail and comments are parsed`() = runBlocking {
        val postListing = RedlibPostScraper(Dispatchers.Unconfined, baseUrl).scrap(DETAIL)
        val post = (postListing.data.children.first() as PostChild).data
        assertEquals("t3_abc123", post.name)
        assertEquals("A title", post.title)
        assertEquals(3100, post.commentsNumber)
        assertEquals(1234, post.score)

        val comments = RedlibCommentScraper(
            Dispatchers.Unconfined,
            baseUrl,
            "t3_abc123",
            "pics"
        ).scrap(DETAIL)

        assertEquals(1, comments.data.children.size)
        val root = comments.data.children.first() as CommentChild
        assertEquals("t1_c1", root.data.name)
        assertEquals("someone", root.data.author)
        assertEquals(12, root.data.score)
        assertTrue(root.data.bodyHtml.contains("body"))

        val reply = root.data.replies!!.data.children.first() as CommentChild
        assertEquals("t1_c2", reply.data.name)
        assertEquals(1, reply.data.depth)
    }

    @Test
    fun `subreddit info is parsed`() = runBlocking {
        val child = RedlibSubredditScraper(Dispatchers.Unconfined, baseUrl).scrap(SUBREDDIT) as AboutChild
        assertEquals("pics", child.data.displayName)
        assertEquals("Pics", child.data.title)
        assertEquals(1234567, child.data.subscribers)
        assertEquals(4321, child.data.activeUserCount)
    }

    @Test
    fun `subreddit search is parsed`() = runBlocking {
        val listing = RedlibSubredditSearchScraper(Dispatchers.Unconfined, baseUrl).scrap(SEARCH)
        assertEquals(1, listing.data.children.size)
        val child = listing.data.children.first() as AboutChild
        assertEquals("pics", child.data.displayName)
        assertEquals(1234567, child.data.subscribers)
        assertNotNull(child.data.url)
    }

    @Test
    fun `user comments are parsed`() = runBlocking {
        val listing = RedlibUserCommentScraper(Dispatchers.Unconfined, baseUrl, "someone").scrap(USER)
        val comment = listing.data.children.first() as CommentChild
        val data: CommentData = comment.data
        assertEquals("someone", data.author)
        assertEquals("Some thread", data.linkTitle)
        assertEquals("pics", data.subreddit)
        assertEquals("t1_c9", data.name)
    }

    companion object {
        private const val HEADER = "<html><body><main><div id=\"column_one\">"

        private const val FOOTER =
            "<footer><a href=\"?sort=hot&t=day&after=t3_next\" accesskey=\"N\">NEXT</a></footer>" +
                "</div></main></body></html>"

        private const val LISTING = HEADER +
            "<div id=\"posts\">" +
            "<div class=\"post\" id=\"img1\">" +
            "<p class=\"post_header\"><a class=\"post_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<a class=\"post_author\" href=\"/u/op\">u/op</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span></p>" +
            "<h2 class=\"post_title\"><a href=\"/r/pics/comments/img1/image_title/\">Image title</a></h2>" +
            "<div class=\"post_media_content\"><a href=\"https://i.redd.it/pic.jpg\" class=\"post_media_image\">" +
            "<img src=\"https://i.redd.it/pic.jpg\"/></a></div>" +
            "<div class=\"post_score\" title=\"1234\">1.2k</div>" +
            "<div class=\"post_footer\"><a href=\"/r/pics/comments/img1/image_title/\" class=\"post_comments\" title=\"56 comments\">56 comments</a></div>" +
            "</div>" +
            "<div class=\"post\" id=\"self1\">" +
            "<p class=\"post_header\"><a class=\"post_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<a class=\"post_author\" href=\"/u/op\">u/op</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span></p>" +
            "<h2 class=\"post_title\"><a href=\"/r/pics/comments/self1/self_title/\">Self title</a></h2>" +
            "<div class=\"post_score\" title=\"10\">10</div>" +
            "<div class=\"post_body post_preview\"><p>self body</p></div>" +
            "<div class=\"post_footer\"><a href=\"/r/pics/comments/self1/self_title/\" class=\"post_comments\" title=\"0 comments\">0 comments</a></div>" +
            "</div>" +
            "<div class=\"post\" id=\"link1\">" +
            "<p class=\"post_header\"><a class=\"post_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<a class=\"post_author\" href=\"/u/op\">u/op</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span></p>" +
            "<h2 class=\"post_title\"><a href=\"/r/pics/comments/link1/link_title/\">Link title</a></h2>" +
            "<a class=\"post_thumbnail\" href=\"https://example.com/story\"><span>example.com</span></a>" +
            "<div class=\"post_score\" title=\"10\">10</div>" +
            "<div class=\"post_footer\"><a href=\"/r/pics/comments/link1/link_title/\" class=\"post_comments\" title=\"2 comments\">2 comments</a></div>" +
            "</div>" +
            "<div class=\"post\" id=\"gal1\">" +
            "<p class=\"post_header\"><a class=\"post_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<a class=\"post_author\" href=\"/u/op\">u/op</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span></p>" +
            "<h2 class=\"post_title\"><a href=\"/r/pics/comments/gal1/gal_title/\">Gallery title</a></h2>" +
            "<div class=\"gallery\">" +
            "<figure><a href=\"https://i.redd.it/one.jpg\"><img src=\"https://i.redd.it/one.jpg\"/></a></figure>" +
            "<figure><a href=\"https://i.redd.it/two.mp4\"><img src=\"https://i.redd.it/two.jpg\"/></a></figure>" +
            "</div>" +
            "<div class=\"post_score\" title=\"10\">10</div>" +
            "<div class=\"post_footer\"><a href=\"/r/pics/comments/gal1/gal_title/\" class=\"post_comments\" title=\"1 comment\">1 comment</a></div>" +
            "</div>" +
            "</div>" + FOOTER

        private const val DETAIL = HEADER +
            "<div class=\"post highlighted\">" +
            "<p class=\"post_header\"><a class=\"post_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<a class=\"post_author\" href=\"/u/op\">u/op</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span></p>" +
            "<h1 class=\"post_title\">A title <small class=\"nsfw\">NSFW</small></h1>" +
            "<div class=\"post_body\"><p>body</p></div>" +
            "<div class=\"post_score\" title=\"1234\">1.2k</div>" +
            "<div class=\"post_footer\"><ul id=\"post_links\"><li><a href=\"/r/pics/comments/abc123/a_title/\">permalink</a></li></ul></div>" +
            "</div>" +
            "<p id=\"comment_count\">3.1k comments <span id=\"sorted_by\">sorted by</span></p>" +
            "<div class=\"thread\">" +
            "<div id=\"c1\" class=\"comment\">" +
            "<div class=\"comment_left\"><p class=\"comment_score\" title=\"12\">12</p><div class=\"line\"></div></div>" +
            "<details class=\"comment_right\" open><summary class=\"comment_data\">" +
            "<a class=\"comment_author op\" href=\"/user/someone\">u/someone</a>" +
            "<a href=\"/r/pics/comments/abc123/a_title/c1/?context=3#c1\" class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</a>" +
            "</summary><div class=\"comment_body\"><p>body</p></div>" +
            "<blockquote class=\"replies\">" +
            "<div id=\"c2\" class=\"comment\">" +
            "<div class=\"comment_left\"><p class=\"comment_score\" title=\"3\">3</p></div>" +
            "<details class=\"comment_right\" open><summary class=\"comment_data\">" +
            "<a class=\"comment_author\" href=\"/user/replier\">u/replier</a>" +
            "<a href=\"/r/pics/comments/abc123/a_title/c2/\" class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</a>" +
            "</summary><div class=\"comment_body\"><p>reply</p></div><blockquote class=\"replies\"></blockquote></details></div>" +
            "</blockquote></details></div>" +
            "</div>" + FOOTER

        private const val SUBREDDIT = "<html><body><main><div id=\"column_one\"></div>" +
            "<aside><details class=\"panel\" id=\"subreddit\" open>" +
            "<summary id=\"subreddit_label\">Subreddit</summary>" +
            "<div id=\"sub_meta\">" +
            "<img loading=\"lazy\" id=\"sub_icon\" src=\"https://styles.redditmedia.com/icon.png\" alt=\"Icon\">" +
            "<h1 id=\"sub_title\">Pics</h1><p id=\"sub_name\">r/pics</p>" +
            "<p id=\"sub_description\">A place for pictures</p>" +
            "<div id=\"sub_details\"><label>Members</label><label>Active</label>" +
            "<div title=\"1234567\">1.2m</div><div title=\"4321\">4.3k</div></div>" +
            "</div></details>" +
            "<details class=\"panel\" id=\"sidebar\"><div id=\"sidebar_contents\"><p>Sidebar</p></div></details>" +
            "</aside></main></body></html>"

        private const val SEARCH = HEADER +
            "<div id=\"search_subreddits\">" +
            "<a href=\"/r/pics\" class=\"search_subreddit\">" +
            "<div class=\"search_subreddit_left\"><img src=\"https://styles.redditmedia.com/icon.png\"></div>" +
            "<div class=\"search_subreddit_right\">" +
            "<p class=\"search_subreddit_header\"><span class=\"search_subreddit_name\">r/pics</span>" +
            "<span class=\"search_subreddit_members\" title=\"1234567\">1.2m Members</span></p>" +
            "<p class=\"search_subreddit_description\">Pictures</p></div></a>" +
            "</div>" + FOOTER

        private const val USER = HEADER +
            "<div id=\"posts\">" +
            "<div class=\"comment user-comment\">" +
            "<div class=\"comment_left\"><p class=\"comment_score\" title=\"7\">7</p></div>" +
            "<details class=\"comment_right\" open><summary class=\"comment_data\">" +
            "<a class=\"comment_link\" href=\"/r/pics/comments/abc123/a_title/c9/#c9\" title=\"Some thread\">Some thread</a>" +
            "<span class=\"created-in\">in</span><a class=\"comment_subreddit\" href=\"/r/pics\">r/pics</a>" +
            "<span class=\"created\" title=\"Nov 11 2026, 06:33:12 UTC\">5h ago</span>" +
            "</summary><p class=\"comment_body\">user comment</p></details></div>" +
            "</div>" + FOOTER
    }
}
