package com.cosmos.unreddit.data.remote.scraper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

abstract class Scraper<Result>(
    private val ioDispatcher: CoroutineDispatcher
) {

    var document: Document? = null
        private set

    open suspend fun scrap(body: String): Result = withContext(ioDispatcher) {
        val document = Jsoup.parse(body)
        scrap(document)
    }

    open suspend fun scrap(document: Document): Result {
        this@Scraper.document = document
        return scrapDocument(document)
    }

    protected abstract suspend fun scrapDocument(document: Document): Result
}
