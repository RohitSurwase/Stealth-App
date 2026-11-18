package com.cosmos.unreddit

import com.cosmos.unreddit.data.model.Block.ImageBlock
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
}
