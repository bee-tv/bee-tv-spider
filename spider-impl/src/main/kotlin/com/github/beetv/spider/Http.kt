package com.github.beetv.spider

import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

suspend fun HttpResponse.text(): String = bodyAsText()

suspend fun HttpResponse.html(): Document = Jsoup.parse(text(), request.url.toString())

suspend fun <T> HttpResponse.html(parser: suspend Document.() -> T): T = parser(html())