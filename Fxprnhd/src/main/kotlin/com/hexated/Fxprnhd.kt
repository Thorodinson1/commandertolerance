package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

class Fxprnhd : MainAPI() {
    override var mainUrl = "https://fxpornhd.com"
    override var name = "Fxprnhd"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/c/bangbros/page/" to "Bang Bros",
        "$mainUrl/c/brazzers/page/" to "Brazzers",
        "$mainUrl/c/realitykings/page/" to "Reality Kings",
        "$mainUrl/c/blacked/page/" to "Blacked",
        "$mainUrl/c/pervmom/page/" to "Pervmom",
       
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home =
            document.select("div.videos-list > article")
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
        val title = this.selectFirst("span.title")?.text() ?: return null
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
                document.select("div.videos-list > article")
                    .mapNotNull {
                        it.toSearchResult()
                    }
            searchResponse.addAll(results)
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        if (data.contains("streamtape")) {
            val doc = app.get(data).document
            doc.select("div.video-infos div:last-child a").map { fixUrl(it.attr("src")) }
                .apmap { source ->
                    safeApiCall {
                        when {
                            source.startsWith("hhttps://streamtape.com") -> app.get(
                                source,
                                referer = "$directUrl/"
                            ).document.select("div.video-infos div:last-child a")
                                .apmap {
                                    loadExtractor(
                                        it.attr("src").substringBefore("=https://streamtape.com"),
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