package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.network.DdosGuardKiller
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

class Fullprn : MainAPI() {
    override var mainUrl = "https://fullporner.com"
    override var name = "Fullprn"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/channel/mylf/page/" to "Mylf",
        "$mainUrl/channel/puretaboo/page/" to "Pure Taboo",
        "$mainUrl/channel/sweetsinner/page/" to "Sweet sinner",
        "$mainUrl/channel/familystrokes/page/" to "FamilyStrokes",
        "$mainUrl/channel/primalfetish/page/" to "PrimalFetish",
       
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home =
            document.select("div.row div.col-xl-3")
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
        val title = this.selectFirst("div.video-title > a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..15) {
            val document =
                app.get(
                    "$mainUrl/search/?s=query&page=$i",
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).document
            val results =
                document.select("div.row div.col-xl-3")
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

        val title = document.selectFirst("div.single-video-title > h2")?.text()?.trim().toString()
        val poster =
            fixUrlNull(document.selectFirst("div.fluid_video_wrapper video]")?.attr("poster").toString())
        val tags = document.select("div.single-video-info-content > b").map { it.text() }
        val description = document.select("div.single-video-info-content > b").text().trim()
        val actors = document.select("div.single-video-info-content > b").map { it.text() }
        
        val recommendations =
            document.select("div.row div.col-xl-3").mapNotNull {
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

        val iframe = app.get(data).document.select("div.responsive-player iframe").attr("src")
        
        if (iframe.startsWith(mainUrl)) {
            val video = app.get(iframe, referer = data).document.select("video source").attr("src")
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