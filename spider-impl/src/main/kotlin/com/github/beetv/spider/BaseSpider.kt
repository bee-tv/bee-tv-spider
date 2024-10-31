package com.github.beetv.spider

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseSpider : Spider {

    open val log: Logger = LoggerFactory.getLogger(this::class.java)

    open val httpClient: HttpClient = HttpClient {
        install(HttpTimeout)
        defaultRequest {
            header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
            )
        }
    }

    open val defaultHttpRequestBuilder: HttpRequestBuilder.() -> Unit = {
        timeout {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

    override suspend fun fetchHotSearchKeyword(): List<String> = emptyList()

    override suspend fun fetchCategory(): List<Category> = emptyList()

    override suspend fun fetchCategoryWithMedia(): List<CategoryWithSimpleMedia> = emptyList()

    override suspend fun fetchMediaByCategory(categoryId: String, pageNum: Long, pageSize: Long): Page<SimpleMedia> =
        emptyPage()

    override suspend fun fetchMediaByTopN(pageNum: Long, pageSize: Long): List<SimpleMedia> = emptyList()

    override suspend fun fetchMediaById(mediaId: String): Media? = null

    override suspend fun searchMedia(keyword: String, pageNum: Long, pageSize: Long): Page<SimpleMedia> = emptyPage()

    override suspend fun resolveMediaUrl(url: String): String = ""

    fun <T> emptyPage(): Page<T> = Page(emptyList(), 0)

    fun <T> pageOf(items: List<T>, total: Long): Page<T> = Page(items, total)

    suspend fun <T> get(
        url: String,
        requestBuilder: HttpRequestBuilder.() -> Unit = defaultHttpRequestBuilder,
        handler: suspend HttpResponse.() -> T
    ): T = withContext(Dispatchers.IO) {
        log.debug("getting: $url")
        handler(httpClient.get(url, requestBuilder))
    }

    suspend fun <T> getAll(
        urls: Collection<String>,
        requestBuilder: HttpRequestBuilder.() -> Unit = defaultHttpRequestBuilder,
        handler: suspend HttpResponse.() -> T
    ): List<T> = coroutineScope {
        urls
            .map {
                async {
                    get(it, requestBuilder, handler)
                }
            }
            .map { it.await() }
    }

    suspend fun <T> Document.follow(
        cssQuery: String,
        requestBuilder: HttpRequestBuilder.() -> Unit = defaultHttpRequestBuilder,
        handler: suspend Document.() -> T
    ): T {
        val url = select(cssQuery).attr("abs:href")
        return get(url, requestBuilder) { handler(html()) }
    }

    suspend fun <T> Document.followAll(
        cssQuery: String,
        requestBuilder: HttpRequestBuilder.() -> Unit = defaultHttpRequestBuilder,
        handler: suspend Document.(Element) -> T
    ): List<T> = coroutineScope {
        select(cssQuery)
            .map {
                val link = it.attr("abs:href")
                async {
                    get(link, requestBuilder) { handler(html(), it) }
                }
            }
            .map { it.await() }
    }
}