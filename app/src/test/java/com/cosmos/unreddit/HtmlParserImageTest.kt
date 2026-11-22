package com.cosmos.unreddit

import com.cosmos.unreddit.data.model.Block.ImageBlock
import com.cosmos.unreddit.data.model.Block.VideoBlock
import com.cosmos.unreddit.data.model.HtmlBlock
import com.cosmos.unreddit.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlParserImageTest {

    @Test
    fun `figure image is parsed as an image block with its fullscreen link`() = runBlocking {
        val text = HtmlParser(Dispatchers.Unconfined).separateHtmlBlocks(
            """<figure><a href="https://redlib.example/img/full.jpg"><img src="https://redlib.example/img/preview.jpg" alt="Example"></a></figure>"""
        )

        assertEquals(1, text.blocks.size)
        assertEquals(HtmlBlock.BlockType.IMAGE, text.blocks.single().type)
        assertEquals(
            ImageBlock(
                "https://redlib.example/img/preview.jpg",
                "https://redlib.example/img/full.jpg",
                "Example"
            ),
            text.blocks.single().block
        )
    }

    @Test
    fun `linked giphy video is parsed as a video block`() = runBlocking {
        val text = HtmlParser(Dispatchers.Unconfined).separateHtmlBlocks(
            """<a href="https://redlib.example/giphy/id/gif"><video class="giphy-embed" src="https://redlib.example/giphy/id/mp4" autoplay muted loop playsinline></video></a>"""
        )

        assertEquals(
            VideoBlock(
                "https://redlib.example/giphy/id/mp4",
                "https://redlib.example/giphy/id/gif"
            ),
            text.blocks.single { it.type == HtmlBlock.BlockType.VIDEO }.block
        )
    }
}
