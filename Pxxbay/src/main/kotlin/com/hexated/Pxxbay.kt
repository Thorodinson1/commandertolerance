package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.*

class Pxxbay : MainAPI() {
    override var mainUrl = "https://www.pxxbay.com"
    override var name = "Pxxbay"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/category/scenes/sd-scenes=" to "SD scences",
        "$mainUrl/category/scenes/720p-scenes=" to "72OP Scences",
        "$mainUrl/category/movies/720p-movies=" to "72OP Movies",
        "$mainUrl/category/movies/1080p-movies=" to "1080p Movies",
       
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home =
            document.select("div.gridmax-grid-posts div.gridmax-grid-post gridmax-5-col, div.gridmax-posts gridmax-posts-grid div.gridmax-grid-post gridmax-4-col")
                .mapNotNull {
                    it.toSearchResult()
                }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a.title")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..15) {
            val document = app.get("$mainUrl/search/1?s=query&page=$i").document
            val results =
                document.select("div.gridmax-posts gridmax-posts-grid")
                    .mapNotNull {
                        it.toSearchResult()
                    }
            searchResponse.addAll(results)
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.post-title entry-title")?.text()?.trim().toString()
        val poster = fixUrlNull(document.selectFirst("a")?.attr("src"))
        val tags = document.select("div.info div:nth-child(5) > a").map { it.text() }
        val description = document.select("div.info div:nth-child(2)").text().trim()
        val actors = document.select("div.info div:nth-child(6) > a").map { it.text() }
        val recommendations =
            document.select("div.gridmax-related-posts-list div.gridmax-related-post-item gridmax-4-col-item").mapNotNull {
                it.toSearchResult()
            }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            addActors(actors)
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains("watchomovies")) {
            val doc = app.get(data).document
            doc.select("div.video-container iframe").map { fixUrl(it.attr("src")) }
                .apmap { source ->
                    safeApiCall {
                        when {
                            source.startsWith("https://doodstream.com") -> app.get(
                                source,
                                referer = "$directUrl/"
                            ).document.select("ul.list-server-items li")
                                .apmap {
                                    loadExtractor(
                                        it.attr("data-video").substringBefore("=https://subload"),
                                        "$directUrl/",
                                        subtitleCallback,
                                        callback
                                    )
                                }
                            else -> loadExtractor(source, "$directUrl/", subtitleCallback, callback)
                        }
                    }
                }
        } else {
            loadExtractor(data, "$directUrl/", subtitleCallback, callback)
        }

        return true
    } 
}

