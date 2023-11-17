package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

class Mlfnut : MainAPI() {
    override var mainUrl = "https://milfnut.com"
    override var name = "Mlfnut"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/category/trending/page/" to "Trending",
        "$mainUrl/category/milf-porn-vids/page/" to "Milf",
        "$mainUrl/category/aunt-nephew-porn/page/" to "Aunt Nepw",
        "$mainUrl/category/incest-porn-vids/page/" to "Icet",
        "$mainUrl/category/mom-son-porn-vids/page" to "M and S",
        "$mainUrl/category/brother-sister-porn-videos/page/" to "B and S",
        "$mainUrl/category/dad-daughter-porn-videos/page/" to "D and D",
        "$mainUrl/category//category/rough-porn/page/" to "Rough",
        "$mainUrl/category/anal-porn-videos/page/" to "Anl",
        "$mainUrl/category/asian-porn-videos/page/" to "Asian",
       
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
        val title = this.selectFirst("span.entry-header")?.text() ?: return null
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
                document.select("div > article")
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

        val title = document.selectFirst("div.title-block >h1")?.text()?.trim().toString()
        val poster =
            fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content").toString())
        val tags = document.select("div.tags-list.a > a").map { it.text() }
        val description = document.select("div.video-description.div > p").text().trim()
        val actors = document.select("div.video-actors > a").map { it.text() }
        
        val recommendations =
            document.select("div.videos-list > article").mapNotNull {
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
                        "Range" to "bytes=12466458-",
                    ),
                )
            )
        } else {
            loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
        }

        return true
    }
}