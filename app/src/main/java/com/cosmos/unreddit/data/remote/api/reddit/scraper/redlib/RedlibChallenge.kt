package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

/**
 * Detects Redlib responses that are not real Reddit content: anti-bot challenge
 * pages (Cloudflare's "Just a moment..." interstitial and the Anubis/within.website
 * check) and meta-refresh redirect pages that bounce the request to another
 * host/subdomain instead of serving the requested listing.
 *
 * These pages answer with HTTP 200 **or** a non-2xx status (e.g. Cloudflare's 403)
 * and put their markers near the top of the document, so a case-insensitive scan
 * of the leading bytes is enough and cheap. The words chosen are specific to the
 * challenge pages and avoid the generic terms (e.g. "cloudflare") that would
 * otherwise cause false positives on real content.
 *
 * Shared by [RedlibScraper] (which parses HTTP-200 bodies) and
 * [com.cosmos.unreddit.data.remote.api.reddit.source.TedditSource] (which must
 * inspect the body of an [retrofit2.HttpException] when the challenge is served
 * with a non-2xx status, since Retrofit throws before the scraper runs).
 */
object RedlibChallenge {

    /** Challenge markers live in <head>, so scanning a prefix is sufficient. */
    private const val CHALLENGE_SAMPLE_BYTES = 20_000

    fun isChallengePage(body: String): Boolean {
        val sample = body.take(CHALLENGE_SAMPLE_BYTES).lowercase()
        return sample.contains("making sure you're not a bot") || // within.website title
            sample.contains("within.website") || // the challenge service
            sample.contains("check your browser") || // Anubis/common challenge text
            sample.contains("just a moment...") || // Cloudflare interstitial title
            sample.contains("checking your browser before accessing") || // Cloudflare
            sample.contains("attention required!") || // Cloudflare blocked page
            sample.contains("i'm under attack") || // Cloudflare attack mode
            sample.contains("<meta http-equiv=\"refresh\"") // redirect, not content
    }

    /**
     * Redlib is a plain HTML proxy; a 403/429 on a listing request is a bot
     * challenge, never legitimate content. Cloudflare serves these with an empty
     * body to non-browser clients (e.g. OkHttp), so the body-marker scan alone
     * misses them — hence the status check. 404/500 are intentionally excluded:
     * 404 is a real "not found" and 500 is a genuine server outage, neither of
     * which inline verification can fix.
     */
    fun isChallenge(code: Int, body: String): Boolean =
        isChallengePage(body) || code == 403 || code == 429
}
