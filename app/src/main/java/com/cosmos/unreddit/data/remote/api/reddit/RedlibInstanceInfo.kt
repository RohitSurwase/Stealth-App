package com.cosmos.unreddit.data.remote.api.reddit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Build identifiers reported by redlib instances in their footer — a
 * `span#version` (e.g. `v0.40.0`) or the `a#source` link text (`version-commit`,
 * forks may emit a bare commit hash such as `82235ec`) — keyed by instance
 * host. Taken as-is from the footer, no parsing.
 *
 * Populated opportunistically from every scraped redlib page — the footer is
 * present on all of them, so no extra request is needed. Values are
 * in-memory only and repopulate on the first scrape after a restart.
 */
object RedlibInstanceInfo {

    private val _versions = MutableStateFlow<Map<String, String>>(emptyMap())
    val versions: StateFlow<Map<String, String>> = _versions

    fun update(host: String, version: String) {
        if (host.isBlank() || version.isBlank()) return
        val current = _versions.value
        if (current[host] == version) return
        _versions.value = current + (host to version)
    }
}
