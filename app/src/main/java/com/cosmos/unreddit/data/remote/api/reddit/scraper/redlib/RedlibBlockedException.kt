package com.cosmos.unreddit.data.remote.api.reddit.scraper.redlib

import java.io.IOException

/**
 * Thrown when a Redlib instance answers with an anti-bot challenge page
 * (within.website / Cloudflare Anubis/Turnstile) instead of real content.
 *
 * These challenges return HTTP 200, so the failure is invisible to the network
 * layer: Jsoup parses the challenge page into an empty listing and the UI would
 * otherwise render a blank screen. Throwing here lets Paging surface the
 * standard "Something went wrong / RETRY" error instead.
 *
 * Extends [java.io.IOException] so it flows through the same error path as every
 * other scrape failure.
 */
class RedlibBlockedException(
    message: String,
    /**
     * The exact listing URL that returned the challenge, so the inline
     * verification can reload the same path (Cloudflare challenges can be
     * path-specific) instead of just the instance root.
     */
    val url: String? = null,
) : IOException(message)
