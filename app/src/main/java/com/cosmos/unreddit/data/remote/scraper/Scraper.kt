package com.cosmos.unreddit.data.remote.scraper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

abstract class Scraper<Result>(
    private val body: String,
    private val ioDispatcher: CoroutineDispatcher
) {

    protected var document: Document? = null

    open suspend fun scrap(): Result = withContext(ioDispatcher) {
        println(body)
        val document = Jsoup.parse(body)
        this@Scraper.document = document
        scrap(document)
    }

    protected abstract suspend fun scrap(document: Document): Result
}
