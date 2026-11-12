package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import com.cosmos.unreddit.data.remote.scraper.Scraper
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Base class for scrapers that parse Redlib HTML.
 *
 * Redlib emits relative links (e.g. `/r/pics/comments/...`) and timestamps
 * formatted as `Nov 11 2026, 06:33:12 UTC` in the `title` attribute. Both are
 * normalised here so the individual scrapers stay small.
 */
abstract class RedlibScraper<Result>(
    ioDispatcher: CoroutineDispatcher,
    protected val baseUrl: String
) : Scraper<Result>(ioDispatcher) {

    protected fun String.toAbsoluteUrl(): String {
        if (isBlank()) return this
        if (startsWith("http://", ignoreCase = true) ||
            startsWith("https://", ignoreCase = true)
        ) {
            return this
        }
        return baseUrl.trimEnd('/') + "/" + trimStart('/')
    }

    protected fun Element.absoluteUrl(attribute: String = "href"): String {
        return attr(attribute).toAbsoluteUrl()
    }

    protected fun Element.timestamp(): Long {
        return parseTimestamp(attr("title").ifBlank { text() })
    }

    protected fun parseTimestamp(value: String?): Long {
        if (value.isNullOrBlank()) return nowInSeconds()

        // Redlib's absolute timestamp, e.g. "Nov 11 2026, 06:33:12 UTC"
        REDLIB_FORMATS.forEach { pattern ->
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(value)?.let {
                    TimeUnit.MILLISECONDS.toSeconds(it.time)
                }
            }.getOrNull()

            if (parsed != null) return parsed
        }

        // Fallback for instances that emit an ISO-8601 timestamp
        return runCatching {
            val normalised = value.replace("Z", "+00:00")
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            formatter.parse(normalised.substringBefore('.').substringBefore('+'))?.let {
                TimeUnit.MILLISECONDS.toSeconds(it.time)
            } ?: 0L
        }.getOrDefault(0L)
    }

    protected fun nowInSeconds(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }

    /**
     * Reads the `after` cursor from the listing footer (`NEXT` link).
     */
    protected fun getNextKey(): String? {
        return document
            ?.selectFirst("footer a[href*=\"after=\"]")
            ?.attr("href")
            ?.toAbsoluteUrl()
            ?.toHttpUrlOrNull()
            ?.queryParameter("after")
    }

    protected fun String.toScore(): Int {
        val cleaned = trim()
        // Redlib uses "Hidden" as the title when the score is hidden
        if (cleaned.equals("Hidden", ignoreCase = true)) return 0
        cleaned.replace(",", "").toIntOrNull()?.let { return it }
        return SCORE_REGEX.find(cleaned)
            ?.value
            ?.replace(",", "")
            ?.toIntOrNull()
            ?: 0
    }

    protected fun String.toCommentCount(): Int {
        // Listings expose the exact count in a title attribute, but the post
        // detail header renders an abbreviated count such as "3.1k comments".
        val raw = COUNT_REGEX.find(this)
            ?.value
            ?.replace(",", "")
            ?.lowercase()
            ?: return 0
        val multiplier = when (raw.lastOrNull()) {
            'k' -> 1_000.0
            'm' -> 1_000_000.0
            else -> 1.0
        }
        val number = raw.removeSuffix("k").removeSuffix("m").toDoubleOrNull() ?: return 0
        return (number * multiplier).toInt()
    }

    override suspend fun scrap(body: String): Result {
        // Redlib anti-bot challenges (and instances that redirect instead of
        // serving content) answer with HTTP 200, so the failure is invisible to
        // the network layer: Jsoup would parse the challenge/redirect page into
        // an empty listing and the UI would render a blank screen. Detect it
        // here and fail loudly so Paging shows the standard error screen.
        if (RedlibChallenge.isChallengePage(body)) {
            throw RedlibBlockedException(
                "Redlib instance returned an anti-bot challenge page instead of content"
            )
        }
        return super.scrap(body)
    }

    companion object {
        private val REDLIB_FORMATS = listOf(
            "MMM d yyyy, HH:mm:ss z",
            "MMM d yyyy, HH:mm:ss",
            "MMM dd yyyy, HH:mm:ss z",
            "MMM dd yyyy, HH:mm:ss",
            "MMM d yyyy",
            "MMM dd yyyy"
        )

        private val SCORE_REGEX = Regex("-?[\\d,]+")
        private val COUNT_REGEX = Regex("[\\d,.]+[kKmM]?")
    }
}
