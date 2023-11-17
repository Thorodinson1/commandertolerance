package com.commandertolerance

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.*

class SpgBag : MainAPI() { 
    override var mainUrl = "https://spankbang.com"
    override var name = "SpgBag"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(
        TvType.NSFW,
        
    )

    override val mainPage = mainPageOf(
        "$mainUrl/trending_videos/page" to "Trending Videos",
        "$mainUrl/upcoming/page/" to "Upcoming",
        "$mainUrl/new_videos/page/" to "New Videos",
        "$mainUrl/most_popular/page/" to "Most Popular",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home =
            document.select("div.video-list.video-rotate div.video-item")
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
        val title = this.selectFirst("a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("cover lazyload")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl  
        }
    }    
        
    override suspend fun search(query: String): List<SearchResponse> {
            val searchResponse = mutableListOf<SearchResponse>()
            for (i in 1..10) {
                val document = app.get("$mainUrl/s/$query&what=1&page=$i").document
                val results = document.select("div.video-list video-rotate video-list-with-ads div.video-item")
                    .mapNotNull {
                        it.toSearchResult()
                    }
                searchResponse.addAll(results)
                if(results.isEmpty()) break
            }
    
            return searchResponse
        }

        override suspend fun load(url: String): LoadResponse {
            val document = app.get(url).document
    
            val title = document.selectFirst("div.headline > h1")?.text()?.trim().toString()
            val poster = fixUrlNull(document.selectFirst("img[class=lazyload player_thumb]")?.attr("src"))
            val tags = document.select("div.info div:nth-child(5) > a").map { it.text() }
            val description = document.select("div.info div:nth-child(2)").text().trim()
            val actors = document.select("div.info div:nth-child(6) > a").map { it.text() }
                      
            val recommendations =
                document.select("div.video-list.video-rotate div video-item").mapNotNull {
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
            val document = app.get(data).document
            document.select("script").find { it.data().contains("var videoList =") }?.data()
            ?.substringAfter("videoList = [")?.substringBefore("];")?.let { data ->
                Regex("\"m3u8\":\"(\\S*?.mp4)\",").findAll(data).map { it.groupValues[1] }
                    .toList()
            }
                  callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                data.replace("\\", ""),
                referer = mainUrl,
                quality = Qualities.Unknown.value,
//                headers = mapOf("Range" to "bytes=0-"),
            )
        )
        return true
     }





}        