package com.example.data

import com.example.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class RadioBrowserService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://at1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    )

    // Curated high-availability stations matching showcase screenshot
    val curatedStations: List<RadioStation> = listOf(
        RadioStation(
            stationuuid = "curated_1live",
            name = "1LIVE",
            slogan = "Popmusik für NRW",
            streamUrl = "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            favicon = "https://www1.wdr.de/radio/1live/resources/img/favicon/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Pop, Chart, Hits",
            dialFrequencyMhz = 105.5f,
            logoBgHex = "#E91E63",
            logoText = "1LIVE",
            isFavorite = true
        ),
        RadioStation(
            stationuuid = "curated_wdr2",
            name = "WDR 2",
            slogan = "Der beste Mix",
            streamUrl = "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            favicon = "https://www1.wdr.de/radio/wdr2/resources/img/favicon/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Pop, News, Rock",
            dialFrequencyMhz = 100.4f,
            logoBgHex = "#1A237E",
            logoText = "WDR 2",
            isFavorite = true
        ),
        RadioStation(
            stationuuid = "curated_wdr4",
            name = "WDR 4",
            slogan = "Das Radio für mehr Gefühl",
            streamUrl = "https://wdr-wdr4-live.icecastssl.wdr.de/wdr/wdr4/live/mp3/128/stream.mp3",
            favicon = "https://www1.wdr.de/radio/wdr4/resources/img/favicon/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Oldies, 80er, Pop",
            dialFrequencyMhz = 103.8f,
            logoBgHex = "#0D47A1",
            logoText = "WDR 4"
        ),
        RadioStation(
            stationuuid = "curated_wdr5",
            name = "WDR 5",
            slogan = "Kultur und Information",
            streamUrl = "https://wdr-wdr5-live.icecastssl.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3",
            favicon = "https://www1.wdr.de/radio/wdr5/resources/img/favicon/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Kultur, Talk, News",
            dialFrequencyMhz = 88.8f,
            logoBgHex = "#2E7D32",
            logoText = "WDR 5"
        ),
        RadioStation(
            stationuuid = "curated_dlf",
            name = "Deutschlandfunk",
            slogan = "Information und Kultur",
            streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
            favicon = "https://www.deutschlandfunk.de/static/img/deutschlandfunk/icons/apple-touch-icon-128x128.png",
            country = "Deutschland",
            tags = "News, Politik, Kultur",
            dialFrequencyMhz = 102.7f,
            logoBgHex = "#0288D1",
            logoText = "DLF"
        ),
        RadioStation(
            stationuuid = "curated_radioeins",
            name = "radioeins",
            slogan = "Rock, Pop und Haltung",
            streamUrl = "https://dispatcher.rndfnk.com/rbb/radioeins/live/mp3/mid",
            favicon = "https://www.radioeins.de/content/dam/rbb/rbb/logos/touch/rad-128.png",
            country = "Deutschland",
            tags = "Alternative, Indie, Rock",
            dialFrequencyMhz = 95.8f,
            logoBgHex = "#EF6C00",
            logoText = "radioeins",
            isFavorite = true
        ),
        RadioStation(
            stationuuid = "curated_swr3",
            name = "SWR3",
            slogan = "Pop & Party",
            streamUrl = "https://liveradio.swr.de/sw282p3/swr3/play.mp3",
            favicon = "https://www.swr3.de/assets/swr3/icons/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Pop, Comedy, Charts",
            dialFrequencyMhz = 99.9f,
            logoBgHex = "#D32F2F",
            logoText = "SWR3"
        ),
        RadioStation(
            stationuuid = "curated_bayern3",
            name = "Bayern 3",
            slogan = "Aktuelle Hits",
            streamUrl = "https://dispatcher.rndfnk.com/br/br3/live/mp3/mid",
            favicon = "https://api.ardmediathek.de/image-service/images/urn:ard:image:52fd25fc45de7c6a?w=512",
            country = "Deutschland",
            tags = "Pop, Hits, Bayern",
            dialFrequencyMhz = 97.3f,
            logoBgHex = "#689F38",
            logoText = "Bayern 3"
        ),
        RadioStation(
            stationuuid = "curated_njoy",
            name = "N-JOY",
            slogan = "New Music Radio",
            streamUrl = "https://icecast.ndr.de/ndr/njoy/live/mp3/128/stream.mp3",
            favicon = "https://www.phonostar.de/images/auto_created/NJOY3184x184.png",
            country = "Deutschland",
            tags = "Youth, Charts, Dance",
            dialFrequencyMhz = 91.2f,
            logoBgHex = "#7CB342",
            logoText = "N-JOY"
        ),
        RadioStation(
            stationuuid = "curated_antenne_bayern",
            name = "Antenne Bayern",
            slogan = "Die besten aktuellen Hits",
            streamUrl = "https://s3-webradio.antenne.de/antenne",
            favicon = "https://www.antenne.de/logos/station-antenne-bayern/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Pop, Hits, Mix",
            dialFrequencyMhz = 101.3f,
            logoBgHex = "#1565C0",
            logoText = "Antenne"
        )
    )

    suspend fun searchStations(
        query: String,
        filter: String = "Alle",
        favoriteStations: List<RadioStation> = emptyList()
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        if (filter == "Favoriten" || filter == "★ Favoriten") {
            return@withContext if (trimmed.isEmpty()) {
                favoriteStations
            } else {
                favoriteStations.filter {
                    it.name.contains(trimmed, ignoreCase = true) ||
                    it.tags.contains(trimmed, ignoreCase = true) ||
                    it.slogan.contains(trimmed, ignoreCase = true)
                }
            }
        }

        val favUuids = favoriteStations.map { it.stationuuid }.toSet()
        val localFiltered = when {
            filter.contains("Pop", ignoreCase = true) -> curatedStations.filter { it.tags.contains("Pop", ignoreCase = true) }
            filter.contains("Rock", ignoreCase = true) -> curatedStations.filter { it.tags.contains("Rock", ignoreCase = true) || it.tags.contains("Alternative", ignoreCase = true) }
            filter.contains("Oldies", ignoreCase = true) || filter.contains("80", ignoreCase = true) -> curatedStations.filter { it.tags.contains("Oldies", ignoreCase = true) }
            filter.contains("News", ignoreCase = true) || filter.contains("Info", ignoreCase = true) || filter.contains("Kultur", ignoreCase = true) -> curatedStations.filter { it.tags.contains("News", ignoreCase = true) || it.tags.contains("Kultur", ignoreCase = true) }
            filter == "DAB+" -> curatedStations.filter { it.name.contains("WDR") || it.name.contains("DLF") || it.name.contains("Deutschlandfunk") || it.name.contains("SWR3") || it.name.contains("Bayern") }
            else -> curatedStations
        }.filter {
            trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) || it.tags.contains(trimmed, ignoreCase = true) || it.slogan.contains(trimmed, ignoreCase = true)
        }

        // Determine tag for online search if query is empty
        val onlineTag = when {
            filter.contains("Pop", ignoreCase = true) -> "pop"
            filter.contains("Rock", ignoreCase = true) -> "rock"
            filter.contains("Oldies", ignoreCase = true) || filter.contains("80", ignoreCase = true) -> "80s"
            filter.contains("News", ignoreCase = true) || filter.contains("Info", ignoreCase = true) -> "news"
            filter.contains("Electro", ignoreCase = true) || filter.contains("Dance", ignoreCase = true) -> "dance"
            filter.contains("Kultur", ignoreCase = true) -> "culture"
            else -> null
        }

        // Attempt live Radio Browser API query across mirrors
        for (mirror in mirrors) {
            try {
                val url = when {
                    trimmed.isNotEmpty() -> {
                        val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
                        "$mirror/json/stations/byname/$encodedQuery?hidebroken=true&limit=35&order=clickcount&reverse=true"
                    }
                    onlineTag != null -> {
                        val encodedTag = URLEncoder.encode(onlineTag, "UTF-8")
                        "$mirror/json/stations/bytag/$encodedTag?hidebroken=true&countrycode=DE&limit=30&order=clickcount&reverse=true"
                    }
                    filter == "DAB+" || filter == "Deutschland" -> {
                        "$mirror/json/stations/bycountry/germany?hidebroken=true&limit=35&order=clickcount&reverse=true"
                    }
                    else -> {
                        "$mirror/json/stations/topclick/35?hidebroken=true"
                    }
                }
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TedokiMixtape/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val jsonArray = JSONArray(body)
                        val apiResults = mutableListOf<RadioStation>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.optString("name")
                            val streamUrl = obj.optString("url_resolved").ifEmpty { obj.optString("url") }
                            if (name.isNotBlank() && streamUrl.startsWith("http")) {
                                val freq = 88.0f + (name.hashCode().mod(200)) / 10.0f
                                val uuid = obj.optString("stationuuid")
                                apiResults.add(
                                    RadioStation(
                                        stationuuid = uuid,
                                        name = name,
                                        slogan = obj.optString("tags").split(",").take(2).joinToString(", "),
                                        streamUrl = streamUrl,
                                        favicon = obj.optString("favicon"),
                                        country = obj.optString("country"),
                                        language = obj.optString("language"),
                                        tags = obj.optString("tags"),
                                        codec = obj.optString("codec", "MP3"),
                                        bitrate = obj.optInt("bitrate", 128),
                                        isFavorite = favUuids.contains(uuid),
                                        dialFrequencyMhz = freq.coerceIn(88.0f, 108.0f),
                                        logoBgHex = "#" + Integer.toHexString(name.hashCode()).takeLast(6).padStart(6, '3'),
                                        logoText = name.take(5)
                                    )
                                )
                            }
                        }
                        if (apiResults.isNotEmpty()) {
                            // Merge with local matches
                            val combined = (localFiltered + apiResults).distinctBy { it.name.lowercase() }
                            return@withContext combined
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next mirror
            }
        }
        return@withContext localFiltered
    }
}
