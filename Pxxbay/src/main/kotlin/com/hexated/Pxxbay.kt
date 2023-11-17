package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
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
        "$mainUrl/category/scenes/sd-scenes/page/" to "SD scences",
        "$mainUrl/category/scenes/720p-scenespage/" to "72OP Scences",
        "$mainUrl/category/movies/720p-movies/page/" to "72OP Movies",
        "$mainUrl/category/movies/1080p-movies/page/" to "1080p Movies",
       
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val home =
            document.select("div.gridmax-posts div.gridmax-grid-post-thumbnail gridmax-grid-post-block")
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
        val tags = document.select("d.entry-content > h4:nth-child(4) > strong:nth-child(1)").map { it.text() }
        val description = document.select("d.entry-content > h4:nth-child(4) > strong:nth-child(1)").text().trim()
        val actors = document.select("dd.entry-content > h4:nth-child(4) > strong:nth-child(1)").map { it.text() }
        val recommendations =
            document.select("div.gridmax-related-posts-list div.gridmax-related-post-item-thumbnail gridmax-related-post-item-child").mapNotNull {
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
        val iframe = app.get(data).document.select("div.video-container iframe").attr("src")
        
        if (iframe.startsWith(mainUrl)) {
            val video = app.get(iframe, referer = data).document.select("div#video_player video").attr("src")
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    video,
                    "$mainUrl/",
                    Qualities.Unknown.value,
                    INFER_TYPE,
                    headers = mapOf(
                        "Range" to "bytes=0-",
                    ),
                )
            )
        } else {
            loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
        }

        return true
    }
}