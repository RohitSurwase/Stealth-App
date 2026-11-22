package com.cosmos.unreddit.util

import android.view.Gravity
import androidx.core.text.HtmlCompat
import com.cosmos.unreddit.data.model.Block.ImageBlock
import com.cosmos.unreddit.data.model.Block.TableBlock
import com.cosmos.unreddit.data.model.Block.TextBlock
import com.cosmos.unreddit.data.model.Block.VideoBlock
import com.cosmos.unreddit.data.model.HtmlBlock
import com.cosmos.unreddit.data.model.RedditText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.LinkedList

class HtmlParser(private val defaultDispatcher: CoroutineDispatcher) {

    private val TABLE_REGEX = Regex("<table>.*?</table>", RegexOption.DOT_MATCHES_ALL)
    private val CODE_REGEX = Regex("<pre><code>(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL)
    private val FIGURE_REGEX = Regex("<figure\\b[^>]*>.*?</figure>", RegexOption.DOT_MATCHES_ALL)
    private val VIDEO_REGEX = Regex(
        """<a\b[^>]*>\s*<video\b[^>]*>.*?</video\s*>\s*</a\s*>|<video\b[^>]*>.*?</video\s*>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val PLACEHOLDER_REGEX = Regex("<(table|code|image|video)_placeholder/>")

    private val tagHandler = RedditTagHandler()

    suspend fun separateHtmlBlocks(html: String?): RedditText = withContext(defaultDispatcher) {
            val redditText = RedditText()

            if (html == null) return@withContext redditText

            val tables = LinkedList<String>()
            val codes = LinkedList<String>()
            val images = LinkedList<ImageBlock>()
            val videos = LinkedList<VideoBlock>()

            var newHtml = html

            newHtml = newHtml.replace(TABLE_REGEX) {
                tables.add(it.groupValues[0])
                TABLE_PLACEHOLDER
            }

            newHtml = newHtml.replace(CODE_REGEX) {
                codes.add(it.groupValues[1])
                CODE_PLACEHOLDER
            }

            newHtml = newHtml.replace(FIGURE_REGEX) {
                val figure = Jsoup.parse(it.value).selectFirst("figure")
                val image = figure?.selectFirst("img[src]")
                val imageUrl = image?.attr("src").orEmpty()
                if (image == null || imageUrl.isBlank()) {
                    it.value
                } else {
                    val linkUrl = image.parents()
                        .firstOrNull { parent -> parent.normalName() == "a" }
                        ?.attr("href")
                        ?.ifBlank { imageUrl }
                        ?: imageUrl
                    images.add(ImageBlock(imageUrl, linkUrl, image.attr("alt").ifBlank { null }))
                    IMAGE_PLACEHOLDER
                }
            }

            newHtml = newHtml.replace(VIDEO_REGEX) {
                val video = Jsoup.parse(it.value).selectFirst("video")
                val sourceUrl = video?.selectFirst("source[src]")?.attr("src").orEmpty()
                val videoUrl = video?.attr("src")?.ifBlank { sourceUrl }.orEmpty()
                if (video == null || videoUrl.isBlank()) {
                    it.value
                } else {
                    val linkUrl = video.parents()
                        .firstOrNull { parent -> parent.normalName() == "a" }
                        ?.attr("href")
                        ?.ifBlank { videoUrl }
                        ?: videoUrl
                    videos.add(VideoBlock(videoUrl, linkUrl))
                    VIDEO_PLACEHOLDER
                }
            }

            if (PLACEHOLDER_REGEX.containsMatchIn(newHtml)) {
                var lastIndex = 0

                for (match in PLACEHOLDER_REGEX.findAll(newHtml)) {
                    if (match.range.first > lastIndex) {
                        val previousBlock = newHtml.substring(lastIndex, match.range.first)
                        redditText.addBlock(getTextBlock(previousBlock), HtmlBlock.BlockType.TEXT)
                    }

                    lastIndex = match.range.last + 1

                    when (match.groupValues[1]) {
                        "table" -> {
                            val tableBlock = getTableFromHtmlTable(tables.pop())
                            redditText.addBlock(tableBlock, HtmlBlock.BlockType.TABLE)
                        }
                        "code" -> {
                            val codeBlock = Parser.unescapeEntities(codes.pop(), true)
                            redditText.addBlock(TextBlock(codeBlock), HtmlBlock.BlockType.CODE)
                        }
                        "image" -> {
                            redditText.addBlock(images.pop(), HtmlBlock.BlockType.IMAGE)
                        }
                        "video" -> {
                            redditText.addBlock(videos.removeFirst(), HtmlBlock.BlockType.VIDEO)
                        }
                    }
                }

                if (lastIndex < newHtml.length) {
                    val lastBlock = newHtml.substring(lastIndex)
                    redditText.addBlock(getTextBlock(lastBlock), HtmlBlock.BlockType.TEXT)
                }
            } else {
                redditText.addBlock(getTextBlock(newHtml), HtmlBlock.BlockType.TEXT)
            }

            return@withContext redditText
    }

    private fun getTableFromHtmlTable(html: String): TableBlock {
        val table = TableBlock()

        val doc = Jsoup.parse(html)

        val rows = doc.select("tr")

        for (row in rows) {
            var isHeader = false

            val cols = if (row.select("td").isNotEmpty()) {
                row.select("td")
            } else {
                isHeader = true
                row.select("th")
            }

            val tableRow = TableBlock.Row(isHeader)

            for (col in cols) {
                val alignment = col.attr("align")

                val spannedHtml = fromHtml(col.html())
                val gravity = getGravityFromAlign(alignment)

                tableRow.addColumn(spannedHtml, gravity)
            }

            table.addRow(tableRow)
        }

        return table
    }

    private fun getGravityFromAlign(align: String): Int {
        return when (align) {
            "left" -> Gravity.START
            "right" -> Gravity.END
            "center" -> Gravity.CENTER
            else -> Gravity.START
        }
    }

    private fun getTextBlock(html: String): TextBlock {
        return TextBlock(fromHtml(html))
    }

    private fun fromHtml(html: String): CharSequence {
        val overriddenHtml = tagHandler.overrideTags(html)
        return HtmlCompat.fromHtml(
            overriddenHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
            null,
            tagHandler
        ).removeSuffix("\n\n")
    }

    companion object {
        private const val TABLE_PLACEHOLDER = "<table_placeholder/>"
        private const val CODE_PLACEHOLDER = "<code_placeholder/>"
        private const val IMAGE_PLACEHOLDER = "<image_placeholder/>"
        private const val VIDEO_PLACEHOLDER = "<video_placeholder/>"
    }
}
