package com.example.data

import com.example.model.RadioStation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Bundesnetzagentur (BNetzA) Open Data FM/UKW Rundfunk-Kataster für Deutschland.
 * Enthält die offiziell koordinierten UKW-Sendeanlagen, Frequenzen (MHz),
 * Sendeleistungen (ERP in kW) und Standorte, verknüpft mit den offiziellen Webstreams.
 */
data class BnetzaStation(
    val id: String,
    val name: String,
    val frequencyMhz: Float,
    val transmitterLocation: String,
    val erpKw: String,
    val category: String,
    val streamUrl: String,
    val slogan: String,
    val logoBgHex: String = "#E53935",
    val txLatitude: Double = 0.0,
    val txLongitude: Double = 0.0,
    val erpKwDouble: Double = 1.0,
    val effectiveHeightMeters: Double = 100.0
) {
    fun toRadioStation(): RadioStation {
        return RadioStation(
            stationuuid = "bnetza_$id",
            name = name,
            streamUrl = streamUrl,
            dialFrequencyMhz = frequencyMhz,
            slogan = slogan,
            logoBgHex = logoBgHex,
            logoText = name.take(4).uppercase(),
            country = "Germany",
            tags = "FM $frequencyMhz MHz, BNetzA $erpKw, $category"
        )
    }
}

data class BnetzaRegion(
    val id: String,
    val name: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val transmitterHubName: String,
    val stations: List<BnetzaStation>
)

object BnetzaFmDatabase {

    private val regions: List<BnetzaRegion> = listOf(
        // 1. KÖLN / BONN (NRW)
        BnetzaRegion(
            id = "koeln_bonn",
            name = "Köln / Bonn",
            state = "Nordrhein-Westfalen",
            latitude = 50.9375,
            longitude = 6.9603,
            transmitterHubName = "Köln-Sternengasse / Bonn-Venusberg",
            stations = listOf(
                BnetzaStation(
                    id = "cgn_wdr5",
                    name = "WDR 5",
                    frequencyMhz = 88.8f,
                    transmitterLocation = "Langenberg / Köln",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr5-live.icecast.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3",
                    slogan = "Das Wortprogramm – Tiefgang & Information",
                    logoBgHex = "#002B49"
                ),
                BnetzaStation(
                    id = "cgn_hr1",
                    name = "hr1",
                    frequencyMhz = 91.0f,
                    transmitterLocation = "Sackpfeife / Biedenkopf",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hr1/live/mp3/high",
                    slogan = "Genau meins – Pop & Oldies",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "cgn_swr1",
                    name = "SWR1 RP",
                    frequencyMhz = 92.4f,
                    transmitterLocation = "Linz am Rhein",
                    erpKw = "50 kW",
                    category = "Öffentlich-rechtlich (SWR)",
                    streamUrl = "https://liveradio.swr.de/sw282p3/swr1rp/mp3/128/stream.mp3",
                    slogan = "Eins gehört gehört – Die größten Hits",
                    logoBgHex = "#0D47A1"
                ),
                BnetzaStation(
                    id = "cgn_wdr2_cgn",
                    name = "WDR 2",
                    frequencyMhz = 93.3f,
                    transmitterLocation = "Bonn / Venusberg",
                    erpKw = "5 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
                    slogan = "WDR 2 Rheinland – Zusammen sind wir der Westen",
                    logoBgHex = "#E53935"
                ),
                BnetzaStation(
                    id = "cgn_wdr3",
                    name = "WDR 3",
                    frequencyMhz = 98.6f,
                    transmitterLocation = "Köln / Sternengasse",
                    erpKw = "1 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr3-live.icecast.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
                    slogan = "Klassik & Kultur fürs Rheinland",
                    logoBgHex = "#5E35B1"
                ),
                BnetzaStation(
                    id = "cgn_wdr2_bonn",
                    name = "WDR 2",
                    frequencyMhz = 100.4f,
                    transmitterLocation = "Köln / Venusberg",
                    erpKw = "50 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
                    slogan = "Der beste Pop & News fürs Rheinland",
                    logoBgHex = "#C62828"
                ),
                BnetzaStation(
                    id = "cgn_1live_bonn",
                    name = "1LIVE",
                    frequencyMhz = 102.4f,
                    transmitterLocation = "Bonn / Venusberg",
                    erpKw = "50 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
                    slogan = "1LIVE – Dein Radio im Sektor",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "cgn_1live_nordhelle",
                    name = "1LIVE",
                    frequencyMhz = 104.7f,
                    transmitterLocation = "Nordhelle",
                    erpKw = "35 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
                    slogan = "1LIVE – Musik & Popkultur",
                    logoBgHex = "#FF6F00"
                ),
                BnetzaStation(
                    id = "cgn_radiobonn",
                    name = "Radio Bonn/Rhein-Sieg",
                    frequencyMhz = 105.8f,
                    transmitterLocation = "Bonn / Venusberg",
                    erpKw = "1 kW",
                    category = "Lokalfunk NRW",
                    streamUrl = "https://stream.lokalradio.nrw/4458h6t",
                    slogan = "Der beste Mix für Bonn und den Rhein-Sieg-Kreis",
                    logoBgHex = "#2E7D32"
                ),
                BnetzaStation(
                    id = "cgn_radiokoeln",
                    name = "Radio Köln",
                    frequencyMhz = 107.1f,
                    transmitterLocation = "Köln-Sternengasse",
                    erpKw = "1 kW",
                    category = "Lokalfunk NRW",
                    streamUrl = "https://stream.lokalradio.nrw/4458jcv",
                    slogan = "Radio Köln 107.1 – Der beste Mix für Kölle",
                    logoBgHex = "#D32F2F"
                )
            )
        ),

        // 2. BERLIN / BRANDENBURG
        BnetzaRegion(
            id = "berlin",
            name = "Berlin / Potsdam",
            state = "Berlin & Brandenburg",
            latitude = 52.5200,
            longitude = 13.4050,
            transmitterHubName = "Alexanderplatz / Scholzplatz",
            stations = listOf(
                BnetzaStation(
                    id = "ber_rbb888",
                    name = "88.8 vom rbb",
                    frequencyMhz = 88.8f,
                    transmitterLocation = "Berlin-Scholzplatz",
                    erpKw = "80 kW",
                    category = "Öffentlich-rechtlich (rbb)",
                    streamUrl = "https://dispatcher.rndfnk.com/rbb/rbb888/live/mp3/mid",
                    slogan = "Guten Morgen Berlin – 88.8 vom rbb",
                    logoBgHex = "#0277BD"
                ),
                BnetzaStation(
                    id = "ber_dlfkultur",
                    name = "Deutschlandfunk Kultur",
                    frequencyMhz = 89.6f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "20 kW",
                    category = "Deutschlandradio",
                    streamUrl = "https://st02.sslstream.dlf.de/dlf/02/128/mp3/stream.mp3",
                    slogan = "Das Kulturprogramm im Deutschlandradio",
                    logoBgHex = "#4527A0"
                ),
                BnetzaStation(
                    id = "ber_brf914",
                    name = "Berliner Rundfunk 91.4",
                    frequencyMhz = 91.4f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "100 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.berliner-rundfunk.de/brf/mp3-128",
                    slogan = "Wir lieben Berlin – Die besten Hits aller Zeiten",
                    logoBgHex = "#C2185B"
                ),
                BnetzaStation(
                    id = "ber_inforadio",
                    name = "rbb Inforadio",
                    frequencyMhz = 93.1f,
                    transmitterLocation = "Scholzplatz",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (rbb)",
                    streamUrl = "https://dispatcher.rndfnk.com/rbb/inforadio/live/mp3/mid",
                    slogan = "Inforadio – Wir lieben das Verstehen",
                    logoBgHex = "#00695C"
                ),
                BnetzaStation(
                    id = "ber_rs2",
                    name = "94,3 rs2",
                    frequencyMhz = 94.3f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "25 kW",
                    category = "Privatfunk",
                    streamUrl = "https://topradio-stream31.radiohost.de/rs2_mp3-192",
                    slogan = "Mein Berlin, meine Musik – 94,3 rs2",
                    logoBgHex = "#F57C00"
                ),
                BnetzaStation(
                    id = "ber_radioeins",
                    name = "radioeins (rbb)",
                    frequencyMhz = 95.8f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (rbb)",
                    streamUrl = "https://dispatcher.rndfnk.com/rbb/radioeins/live/mp3/mid",
                    slogan = "radioeins – Nur für Erwachsene",
                    logoBgHex = "#D84315"
                ),
                BnetzaStation(
                    id = "ber_dlf",
                    name = "Deutschlandfunk",
                    frequencyMhz = 97.7f,
                    transmitterLocation = "Scholzplatz",
                    erpKw = "80 kW",
                    category = "Deutschlandradio",
                    streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
                    slogan = "Alles von Relevanz – Nachrichten & Hintergrund",
                    logoBgHex = "#37474F"
                ),
                BnetzaStation(
                    id = "ber_fritz",
                    name = "Fritz vom rbb",
                    frequencyMhz = 102.6f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "15 kW",
                    category = "Öffentlich-rechtlich (rbb)",
                    streamUrl = "https://dispatcher.rndfnk.com/rbb/fritz/live/mp3/mid",
                    slogan = "Und das hört man – Fritz",
                    logoBgHex = "#2E7D32"
                ),
                BnetzaStation(
                    id = "ber_1046rtl",
                    name = "104.6 RTL",
                    frequencyMhz = 104.6f,
                    transmitterLocation = "Alexanderplatz",
                    erpKw = "10 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.104.6rtl.com/rtl-live/mp3-128",
                    slogan = "Berlins Hitradio – Arno und die Morgencrew",
                    logoBgHex = "#D32F2F"
                ),
                BnetzaStation(
                    id = "ber_bbradio",
                    name = "BB Radio",
                    frequencyMhz = 107.5f,
                    transmitterLocation = "Schäferberg",
                    erpKw = "13 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.bbradio.de/bbradio/mp3-128",
                    slogan = "Voll das Leben – BB Radio",
                    logoBgHex = "#FFB300"
                )
            )
        ),

        // 3. HAMBURG / NORDDEUTSCHLAND
        BnetzaRegion(
            id = "hamburg",
            name = "Hamburg",
            state = "Hamburg",
            latitude = 53.5511,
            longitude = 9.9937,
            transmitterHubName = "Heinrich-Hertz-Turm / Moorfleet",
            stations = listOf(
                BnetzaStation(
                    id = "ham_ndr2",
                    name = "NDR 2",
                    frequencyMhz = 87.6f,
                    transmitterLocation = "Hamburg-Moorfleet",
                    erpKw = "80 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndr2/hamburg/mp3/128/stream.mp3",
                    slogan = "NDR 2 – Genau meine Musik",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "ham_ndr903",
                    name = "NDR 90,3",
                    frequencyMhz = 90.3f,
                    transmitterLocation = "Moorfleet",
                    erpKw = "80 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndr903/live/mp3/128/stream.mp3",
                    slogan = "Wir sind Hamburg – NDR 90,3",
                    logoBgHex = "#F57C00"
                ),
                BnetzaStation(
                    id = "ham_ndrinfo",
                    name = "NDR Info",
                    frequencyMhz = 92.3f,
                    transmitterLocation = "Moorfleet",
                    erpKw = "5 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndrinfo/hamburg/mp3/128/stream.mp3",
                    slogan = "Das Informationsprogramm des Nordens",
                    logoBgHex = "#1565C0"
                ),
                BnetzaStation(
                    id = "ham_njoy",
                    name = "N-JOY",
                    frequencyMhz = 94.2f,
                    transmitterLocation = "Heinrich-Hertz-Turm",
                    erpKw = "1 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/njoy/live/mp3/128/stream.mp3",
                    slogan = "N-JOY vom NDR – Enjoy the Music",
                    logoBgHex = "#E64A19"
                ),
                BnetzaStation(
                    id = "ham_energy",
                    name = "ENERGY Hamburg",
                    frequencyMhz = 97.1f,
                    transmitterLocation = "Heinrich-Hertz-Turm",
                    erpKw = "0.1 kW",
                    category = "Privatfunk",
                    streamUrl = "https://scdn.nrjaudio.fm/audio1/de/30501/mp3_128.mp3",
                    slogan = "Hit Music Only – Energy 97.1",
                    logoBgHex = "#C62828"
                ),
                BnetzaStation(
                    id = "ham_ndrkultur",
                    name = "NDR Kultur",
                    frequencyMhz = 99.2f,
                    transmitterLocation = "Moorfleet",
                    erpKw = "80 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndrkultur/live/mp3/128/stream.mp3",
                    slogan = "Klassik & Kultur für den Norden",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "ham_radiohh",
                    name = "Radio Hamburg",
                    frequencyMhz = 103.6f,
                    transmitterLocation = "Moorfleet",
                    erpKw = "80 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radiohamburg.de/rhh-live/mp3-128",
                    slogan = "Radio Hamburg – Mehr Vielfalt, mehr gute Laune",
                    logoBgHex = "#E53935"
                ),
                BnetzaStation(
                    id = "ham_rockantenne",
                    name = "Rock Antenne Hamburg",
                    frequencyMhz = 106.8f,
                    transmitterLocation = "Rahlstedt",
                    erpKw = "40 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.rockantenne.hamburg/rockantenne-hamburg/mp3-128",
                    slogan = "Der beste Rock für Hamburg",
                    logoBgHex = "#212121"
                )
            )
        ),

        // 4. MÜNCHEN / SÜDBAYERN
        BnetzaRegion(
            id = "muenchen",
            name = "München",
            state = "Bayern",
            latitude = 48.1351,
            longitude = 11.5820,
            transmitterHubName = "Olympiaturm / Wendelstein",
            stations = listOf(
                BnetzaStation(
                    id = "muc_br2",
                    name = "Bayern 2",
                    frequencyMhz = 89.5f,
                    transmitterLocation = "Wendelstein",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br2/sued/mp3/mid",
                    slogan = "Bayern 2 – Hören, was die Welt bewegt",
                    logoBgHex = "#0277BD"
                ),
                BnetzaStation(
                    id = "muc_br1",
                    name = "Bayern 1",
                    frequencyMhz = 90.9f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br1/obb/mp3/mid",
                    slogan = "Bayern 1 – Die beste Musik für Bayern",
                    logoBgHex = "#1565C0"
                ),
                BnetzaStation(
                    id = "muc_energy",
                    name = "ENERGY München",
                    frequencyMhz = 93.3f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "0.3 kW",
                    category = "Privatfunk",
                    streamUrl = "https://scdn.nrjaudio.fm/audio1/de/30401/mp3_128.mp3",
                    slogan = "Hit Music Only – Energy 93.3",
                    logoBgHex = "#D32F2F"
                ),
                BnetzaStation(
                    id = "muc_charivari",
                    name = "Radio Charivari 95.5",
                    frequencyMhz = 95.5f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "0.3 kW",
                    category = "Privatfunk München",
                    streamUrl = "https://edge63.streamonkey.net/charivari-muenchen/stream/mp3",
                    slogan = "Münchens Hitradio 95.5",
                    logoBgHex = "#E64A19"
                ),
                BnetzaStation(
                    id = "muc_gong",
                    name = "Radio Gong 96.3",
                    frequencyMhz = 96.3f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "0.3 kW",
                    category = "Privatfunk München",
                    streamUrl = "https://edge63.streamonkey.net/gong963-live/stream/mp3",
                    slogan = "Radio Gong 96.3 – Dein München Radio",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "muc_br3",
                    name = "Bayern 3",
                    frequencyMhz = 97.3f,
                    transmitterLocation = "Ismaning",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br3/live/mp3/mid",
                    slogan = "Bayern 3 – Dein Bayern, dein Radio",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "muc_arabella",
                    name = "Radio Arabella",
                    frequencyMhz = 98.5f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "0.1 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radioarabella.de/arabella-live/mp3-128",
                    slogan = "Radio Arabella – Kulthits und das Beste von heute",
                    logoBgHex = "#AD1457"
                ),
                BnetzaStation(
                    id = "muc_antenneby",
                    name = "Antenne Bayern",
                    frequencyMhz = 101.3f,
                    transmitterLocation = "Olympiaturm",
                    erpKw = "0.3 kW",
                    category = "Privatfunk Bayern",
                    streamUrl = "https://stream.antenne.de/antenne/stream/mp3",
                    slogan = "Antenne Bayern – Bayerns bester Musikmix",
                    logoBgHex = "#FF6F00"
                ),
                BnetzaStation(
                    id = "muc_br24",
                    name = "BR24",
                    frequencyMhz = 104.0f,
                    transmitterLocation = "München-Blutenburg",
                    erpKw = "0.1 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br24/live/mp3/mid",
                    slogan = "BR24 – Nachrichten rund um die Uhr",
                    logoBgHex = "#37474F"
                ),
                BnetzaStation(
                    id = "muc_antenne_hochries",
                    name = "Antenne Bayern",
                    frequencyMhz = 107.7f,
                    transmitterLocation = "Hochries",
                    erpKw = "50 kW",
                    category = "Privatfunk Bayern",
                    streamUrl = "https://stream.antenne.de/antenne/stream/mp3",
                    slogan = "Antenne Bayern – Große Reichweite Süd",
                    logoBgHex = "#E65100"
                )
            )
        ),

        // 5. FRANKFURT AM MAIN / HESSEN
        BnetzaRegion(
            id = "frankfurt",
            name = "Frankfurt am Main",
            state = "Hessen",
            latitude = 50.1109,
            longitude = 8.6821,
            transmitterHubName = "Europaturm / Großer Feldberg",
            stations = listOf(
                BnetzaStation(
                    id = "fra_hr2",
                    name = "hr2-kultur",
                    frequencyMhz = 87.9f,
                    transmitterLocation = "Frankfurt / Europaturm",
                    erpKw = "0.1 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hr2/live/mp3/high",
                    slogan = "Klassik, Hörspiel & Literatur",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "fra_hr3",
                    name = "hr3",
                    frequencyMhz = 89.3f,
                    transmitterLocation = "Großer Feldberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hr3/live/mp3/high",
                    slogan = "hr3 – Zusammen sind wir Hessen",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "fra_hr1",
                    name = "hr1",
                    frequencyMhz = 94.4f,
                    transmitterLocation = "Großer Feldberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hr1/live/mp3/high",
                    slogan = "Genau meins – Songs & Stories",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "fra_radiofra",
                    name = "Radio Frankfurt",
                    frequencyMhz = 97.6f,
                    transmitterLocation = "Europaturm",
                    erpKw = "0.5 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radio-frankfurt.de/rf-live/mp3-128",
                    slogan = "Die beste Musik für Mainhattan",
                    logoBgHex = "#D81B60"
                ),
                BnetzaStation(
                    id = "fra_planet",
                    name = "planet radio",
                    frequencyMhz = 100.2f,
                    transmitterLocation = "Frankfurt am Main",
                    erpKw = "1 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.planetradio.de/planet/mp3-128",
                    slogan = "discover the music – planet radio",
                    logoBgHex = "#7B1FA2"
                ),
                BnetzaStation(
                    id = "fra_radiobob",
                    name = "Radio BOB!",
                    frequencyMhz = 101.4f,
                    transmitterLocation = "Frankfurt",
                    erpKw = "0.2 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radiobob.de/bob-live/mp3-128",
                    slogan = "BOBs Rock'n'Pop – Mehr Rock braucht kein Mensch",
                    logoBgHex = "#212121"
                ),
                BnetzaStation(
                    id = "fra_hr4",
                    name = "hr4",
                    frequencyMhz = 102.5f,
                    transmitterLocation = "Großer Feldberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hr4/sued/mp3/high",
                    slogan = "hr4 – Mein schönstes Gefühl",
                    logoBgHex = "#EF6C00"
                ),
                BnetzaStation(
                    id = "fra_hrinfo",
                    name = "hr-iNFO",
                    frequencyMhz = 103.9f,
                    transmitterLocation = "Frankfurt",
                    erpKw = "0.5 kW",
                    category = "Öffentlich-rechtlich (hr)",
                    streamUrl = "https://dispatcher.rndfnk.com/hr/hrinfo/live/mp3/high",
                    slogan = "hr-iNFO – Wer es hört, hat mehr zu sagen",
                    logoBgHex = "#00695C"
                ),
                BnetzaStation(
                    id = "fra_ffh",
                    name = "Hit Radio FFH",
                    frequencyMhz = 105.9f,
                    transmitterLocation = "Großer Feldberg",
                    erpKw = "100 kW",
                    category = "Privatfunk Hessen",
                    streamUrl = "https://stream.ffh.de/ffh/mp3-128",
                    slogan = "Einfach näher dran – Hit Radio FFH",
                    logoBgHex = "#D32F2F"
                )
            )
        ),

        // 6. STUTTGART / BADEN-WÜRTTEMBERG
        BnetzaRegion(
            id = "stuttgart",
            name = "Stuttgart",
            state = "Baden-Württemberg",
            latitude = 48.7758,
            longitude = 9.1829,
            transmitterHubName = "Frauenkopf / Fernsehturm",
            stations = listOf(
                BnetzaStation(
                    id = "str_swrkultur",
                    name = "SWR Kultur",
                    frequencyMhz = 87.9f,
                    transmitterLocation = "Stuttgart-Degerloch",
                    erpKw = "1 kW",
                    category = "Öffentlich-rechtlich (SWR)",
                    streamUrl = "https://liveradio.swr.de/sw282p3/swrkultur/mp3/128/stream.mp3",
                    slogan = "Das Kulturprogramm des Südwestrundfunks",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "str_swr4",
                    name = "SWR4 BW",
                    frequencyMhz = 90.1f,
                    transmitterLocation = "Frauenkopf",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (SWR)",
                    streamUrl = "https://liveradio.swr.de/sw282p3/swr4bw/mp3/128/stream.mp3",
                    slogan = "SWR4 Baden-Württemberg – Da sind wir daheim",
                    logoBgHex = "#F57C00"
                ),
                BnetzaStation(
                    id = "str_swr3",
                    name = "SWR3",
                    frequencyMhz = 92.2f,
                    transmitterLocation = "Stuttgart-Degerloch",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (SWR)",
                    streamUrl = "https://liveradio.swr.de/sw282p3/swr3/mp3/128/stream.mp3",
                    slogan = "SWR3 – Mehr Hits, mehr Kicks, einfach SWR3",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "str_swr1",
                    name = "SWR1 BW",
                    frequencyMhz = 94.7f,
                    transmitterLocation = "Stuttgart-Degerloch",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (SWR)",
                    streamUrl = "https://liveradio.swr.de/sw282p3/swr1bw/mp3/128/stream.mp3",
                    slogan = "Eins gehört gehört – SWR1",
                    logoBgHex = "#0D47A1"
                ),
                BnetzaStation(
                    id = "str_antenne1",
                    name = "Hitradio Antenne 1",
                    frequencyMhz = 101.3f,
                    transmitterLocation = "Frauenkopf",
                    erpKw = "75 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.antenne1.de/a1/mp3-128",
                    slogan = "Hier für Euch – Hitradio Antenne 1",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "str_dieneue1077",
                    name = "DIE NEUE 107.7",
                    frequencyMhz = 107.7f,
                    transmitterLocation = "Frauenkopf",
                    erpKw = "4 kW",
                    category = "Privatfunk",
                    streamUrl = "https://dieneue1077.cast.addradio.de/dieneue1077/simulcast/mp3/high",
                    slogan = "Bester Rock und Pop für Stuttgart und die Region",
                    logoBgHex = "#D32F2F"
                )
            )
        ),

        // 7. DÜSSELDORF / RUHRGEBIET (NRW)
        BnetzaRegion(
            id = "duesseldorf",
            name = "Düsseldorf / Ruhrgebiet",
            state = "Nordrhein-Westfalen",
            latitude = 51.2277,
            longitude = 6.7735,
            transmitterHubName = "Rheinturm / Langenberg",
            stations = listOf(
                BnetzaStation(
                    id = "dus_wdr5",
                    name = "WDR 5",
                    frequencyMhz = 88.8f,
                    transmitterLocation = "Langenberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr5-live.icecast.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3",
                    slogan = "WDR 5 – Neugier genügt",
                    logoBgHex = "#002B49"
                ),
                BnetzaStation(
                    id = "dus_dlfkultur",
                    name = "Deutschlandfunk Kultur",
                    frequencyMhz = 96.5f,
                    transmitterLocation = "Langenberg",
                    erpKw = "50 kW",
                    category = "Deutschlandradio",
                    streamUrl = "https://st02.sslstream.dlf.de/dlf/02/128/mp3/stream.mp3",
                    slogan = "Kultur & Diskurs im Deutschlandradio",
                    logoBgHex = "#4527A0"
                ),
                BnetzaStation(
                    id = "dus_wdr2",
                    name = "WDR 2",
                    frequencyMhz = 99.2f,
                    transmitterLocation = "Langenberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
                    slogan = "WDR 2 – Die meisten Hörer im Westen",
                    logoBgHex = "#C62828"
                ),
                BnetzaStation(
                    id = "dus_cosmo",
                    name = "COSMO",
                    frequencyMhz = 103.3f,
                    transmitterLocation = "Langenberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-cosmo-live.icecast.wdr.de/wdr/cosmo/live/mp3/128/stream.mp3",
                    slogan = "COSMO – Global Sounds & Voices",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "dus_antennedus",
                    name = "Antenne Düsseldorf",
                    frequencyMhz = 104.2f,
                    transmitterLocation = "Rheinturm",
                    erpKw = "2 kW",
                    category = "Lokalfunk NRW",
                    streamUrl = "https://stream.lokalradio.nrw/4458h63",
                    slogan = "Antenne Düsseldorf 104.2 – Der beste Mix",
                    logoBgHex = "#D32F2F"
                ),
                BnetzaStation(
                    id = "dus_1live",
                    name = "1LIVE",
                    frequencyMhz = 106.7f,
                    transmitterLocation = "Langenberg",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (WDR)",
                    streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
                    slogan = "1LIVE – Volle Power aus Langenberg",
                    logoBgHex = "#FF8F00"
                )
            )
        ),

        // 8. LEIPZIG / MITTELDEUTSCHLAND
        BnetzaRegion(
            id = "leipzig",
            name = "Leipzig / Halle",
            state = "Sachsen & Sachsen-Anhalt",
            latitude = 51.3397,
            longitude = 12.3731,
            transmitterHubName = "Wiederau / Holzhausen",
            stations = listOf(
                BnetzaStation(
                    id = "lei_mdrkultur",
                    name = "MDR Kultur",
                    frequencyMhz = 88.4f,
                    transmitterLocation = "Wiederau",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (MDR)",
                    streamUrl = "https://mdr-284310-0.cast.mdr.de/mdr/284310/0/mp3/high/stream.mp3",
                    slogan = "MDR Kultur – Das Radio",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "lei_mdrjump",
                    name = "MDR JUMP",
                    frequencyMhz = 90.4f,
                    transmitterLocation = "Wiederau",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (MDR)",
                    streamUrl = "https://mdr-284320-0.cast.mdr.de/mdr/284320/0/mp3/high/stream.mp3",
                    slogan = "MDR JUMP – Wir lieben Popmusik",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "lei_mdrsachsen",
                    name = "MDR Sachsen",
                    frequencyMhz = 93.9f,
                    transmitterLocation = "Leipzig",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (MDR)",
                    streamUrl = "https://mdr-284280-0.cast.mdr.de/mdr/284280/0/mp3/high/stream.mp3",
                    slogan = "Das Sachsenradio – Bei uns daheim",
                    logoBgHex = "#00695C"
                ),
                BnetzaStation(
                    id = "lei_radiopsr",
                    name = "Radio PSR",
                    frequencyMhz = 98.9f,
                    transmitterLocation = "Wiederau",
                    erpKw = "100 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radiopsr.de/psr-live/mp3-128",
                    slogan = "Radio PSR – Sachsens bester Musikmix",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "lei_rtl",
                    name = "Hitradio RTL Sachsen",
                    frequencyMhz = 104.9f,
                    transmitterLocation = "Wiederau",
                    erpKw = "100 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.hitradio-rtl.de/hrrtl/mp3-128",
                    slogan = "Hitradio RTL – Sachsens meiste Hits",
                    logoBgHex = "#D32F2F"
                ),
                BnetzaStation(
                    id = "lei_energy",
                    name = "ENERGY Sachsen",
                    frequencyMhz = 106.9f,
                    transmitterLocation = "Leipzig",
                    erpKw = "2 kW",
                    category = "Privatfunk",
                    streamUrl = "https://scdn.nrjaudio.fm/audio1/de/30601/mp3_128.mp3",
                    slogan = "ENERGY Sachsen – Hit Music Only",
                    logoBgHex = "#E53935"
                )
            )
        ),

        // 9. HANNOVER / NIEDERSACHSEN
        BnetzaRegion(
            id = "hannover",
            name = "Hannover",
            state = "Niedersachsen",
            latitude = 52.3759,
            longitude = 9.7320,
            transmitterHubName = "Telemax",
            stations = listOf(
                BnetzaStation(
                    id = "han_ndrinfo",
                    name = "NDR Info",
                    frequencyMhz = 88.6f,
                    transmitterLocation = "Telemax",
                    erpKw = "0.5 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndrinfo/niedersachsen/mp3/128/stream.mp3",
                    slogan = "NDR Info – Nachrichten im Minutentakt",
                    logoBgHex = "#1565C0"
                ),
                BnetzaStation(
                    id = "han_ndr1",
                    name = "NDR 1 Niedersachsen",
                    frequencyMhz = 90.9f,
                    transmitterLocation = "Telemax",
                    erpKw = "15 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndr1niedersachsen/hannover/mp3/128/stream.mp3",
                    slogan = "NDR 1 Niedersachsen – Das ist Radio!",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "han_njoy",
                    name = "N-JOY",
                    frequencyMhz = 92.6f,
                    transmitterLocation = "Telemax",
                    erpKw = "2.5 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/njoy/live/mp3/128/stream.mp3",
                    slogan = "N-JOY – Dein Radio im Norden",
                    logoBgHex = "#E64A19"
                ),
                BnetzaStation(
                    id = "han_ndr2",
                    name = "NDR 2",
                    frequencyMhz = 96.2f,
                    transmitterLocation = "Telemax",
                    erpKw = "5 kW",
                    category = "Öffentlich-rechtlich (NDR)",
                    streamUrl = "https://icecast.ndr.de/ndr/ndr2/niedersachsen/mp3/128/stream.mp3",
                    slogan = "NDR 2 – Genau meine Musik",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "han_ffn",
                    name = "radio ffn",
                    frequencyMhz = 101.9f,
                    transmitterLocation = "Telemax",
                    erpKw = "25 kW",
                    category = "Privatfunk",
                    streamUrl = "https://ffn-stream28.radiohost.de/ffn_mp3-128",
                    slogan = "radio ffn – Niedersachsens beste Musik",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "han_antennends",
                    name = "Antenne Niedersachsen",
                    frequencyMhz = 103.8f,
                    transmitterLocation = "Telemax",
                    erpKw = "25 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.antenne.com/antenne-nds/mp3-128",
                    slogan = "Antenne Niedersachsen – Mehr Vielfalt",
                    logoBgHex = "#D32F2F"
                )
            )
        ),

        // 10. NÜRNBERG / FRANKEN
        BnetzaRegion(
            id = "nuernberg",
            name = "Nürnberg",
            state = "Bayern",
            latitude = 49.4521,
            longitude = 11.0767,
            transmitterHubName = "Fernmeldeturm Nürnberg / Dillberg",
            stations = listOf(
                BnetzaStation(
                    id = "nue_br2",
                    name = "Bayern 2",
                    frequencyMhz = 88.4f,
                    transmitterLocation = "Pfaffenberg / Nürnberg",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br2/nord/mp3/mid",
                    slogan = "Bayern 2 – Hören, was die Welt bewegt",
                    logoBgHex = "#0277BD"
                ),
                BnetzaStation(
                    id = "nue_brklassik",
                    name = "BR-Klassik",
                    frequencyMhz = 90.1f,
                    transmitterLocation = "Dillberg",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/brklassik/live/mp3/mid",
                    slogan = "Klassische Musik auf höchstem Niveau",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "nue_radiof",
                    name = "Radio F",
                    frequencyMhz = 94.5f,
                    transmitterLocation = "Nürnberg-Fernmeldeturm",
                    erpKw = "0.3 kW",
                    category = "Privatfunk Franken",
                    streamUrl = "https://stream.funkhaus.de/radio-f/mp3-128",
                    slogan = "Radio F – Franken feiert die Hits",
                    logoBgHex = "#C2185B"
                ),
                BnetzaStation(
                    id = "nue_charivari",
                    name = "Radio Charivari 98.6",
                    frequencyMhz = 98.6f,
                    transmitterLocation = "Nürnberg",
                    erpKw = "0.3 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.funkhaus.de/charivari986/mp3-128",
                    slogan = "Mein Nürnberg – Charivari 98.6",
                    logoBgHex = "#E64A19"
                ),
                BnetzaStation(
                    id = "nue_br3",
                    name = "Bayern 3",
                    frequencyMhz = 99.2f,
                    transmitterLocation = "Dillberg",
                    erpKw = "25 kW",
                    category = "Öffentlich-rechtlich (BR)",
                    streamUrl = "https://dispatcher.rndfnk.com/br/br3/live/mp3/mid",
                    slogan = "Bayern 3 – Dein Bayern, dein Radio",
                    logoBgHex = "#00838F"
                ),
                BnetzaStation(
                    id = "nue_gong",
                    name = "Radio Gong 97.1",
                    frequencyMhz = 104.5f,
                    transmitterLocation = "Nürnberg",
                    erpKw = "0.3 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.funkhaus.de/gong971/mp3-128",
                    slogan = "Gong 97.1 – Der echte Rocksender",
                    logoBgHex = "#212121"
                )
            )
        ),

        // 11. DRESDEN / SACHSEN
        BnetzaRegion(
            id = "dresden",
            name = "Dresden",
            state = "Sachsen",
            latitude = 51.0504,
            longitude = 13.7373,
            transmitterHubName = "Fernsehturm Wachwitz",
            stations = listOf(
                BnetzaStation(
                    id = "drs_rsa",
                    name = "R.SA",
                    frequencyMhz = 89.2f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "2 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.rsa-sachsen.de/rsa-live/mp3-128",
                    slogan = "Mit Böttcher und Fischer – R.SA",
                    logoBgHex = "#D81B60"
                ),
                BnetzaStation(
                    id = "drs_mdrsachsen",
                    name = "MDR Sachsen",
                    frequencyMhz = 92.2f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (MDR)",
                    streamUrl = "https://mdr-284280-0.cast.mdr.de/mdr/284280/0/mp3/high/stream.mp3",
                    slogan = "MDR Sachsen – Bei uns daheim",
                    logoBgHex = "#00695C"
                ),
                BnetzaStation(
                    id = "drs_mdrkultur",
                    name = "MDR Kultur",
                    frequencyMhz = 95.4f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (MDR)",
                    streamUrl = "https://mdr-284310-0.cast.mdr.de/mdr/284310/0/mp3/high/stream.mp3",
                    slogan = "MDR Kultur – Das Radio",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "drs_psr",
                    name = "Radio PSR",
                    frequencyMhz = 100.0f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "5 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radiopsr.de/psr-live/mp3-128",
                    slogan = "Radio PSR – Sachsens meiste Hits",
                    logoBgHex = "#FF8F00"
                ),
                BnetzaStation(
                    id = "drs_radiodd",
                    name = "Radio Dresden",
                    frequencyMhz = 103.5f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "2 kW",
                    category = "Lokalradio",
                    streamUrl = "https://stream.radiodresden.de/dresden/mp3-128",
                    slogan = "Die beste Musik für Dresden",
                    logoBgHex = "#0288D1"
                ),
                BnetzaStation(
                    id = "drs_hitrtl",
                    name = "Hitradio RTL",
                    frequencyMhz = 105.2f,
                    transmitterLocation = "Wachwitz",
                    erpKw = "100 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.hitradio-rtl.de/hrrtl/mp3-128",
                    slogan = "Hitradio RTL – Sachsens Hitradio",
                    logoBgHex = "#D32F2F"
                )
            )
        ),

        // 12. BREMEN / NORDWEST
        BnetzaRegion(
            id = "bremen",
            name = "Bremen",
            state = "Bremen",
            latitude = 53.0793,
            longitude = 8.8017,
            transmitterHubName = "Fernmeldeturm Bremen-Walle",
            stations = listOf(
                BnetzaStation(
                    id = "bre_eins",
                    name = "Bremen Eins",
                    frequencyMhz = 88.3f,
                    transmitterLocation = "Bremen-Walle",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (Radio Bremen)",
                    streamUrl = "https://icecast.radiobremen.de/rb/bremeneins/live/mp3/128/stream.mp3",
                    slogan = "Bremen Eins – Die größten Hits aller Zeiten",
                    logoBgHex = "#0277BD"
                ),
                BnetzaStation(
                    id = "bre_zwei",
                    name = "Bremen Zwei",
                    frequencyMhz = 93.8f,
                    transmitterLocation = "Bremen-Walle",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (Radio Bremen)",
                    streamUrl = "https://icecast.radiobremen.de/rb/bremenzwei/live/mp3/128/stream.mp3",
                    slogan = "Bremen Zwei – Kultur, Gespräche & Musik",
                    logoBgHex = "#4A148C"
                ),
                BnetzaStation(
                    id = "bre_next",
                    name = "Bremen NEXT",
                    frequencyMhz = 96.7f,
                    transmitterLocation = "Bremen-Walle",
                    erpKw = "50 kW",
                    category = "Öffentlich-rechtlich (Radio Bremen)",
                    streamUrl = "https://icecast.radiobremen.de/rb/bremennext/live/mp3/128/stream.mp3",
                    slogan = "Bremen NEXT – Deutschrap, Hip Hop & Electronic",
                    logoBgHex = "#00E676"
                ),
                BnetzaStation(
                    id = "bre_vier",
                    name = "Bremen Vier",
                    frequencyMhz = 101.2f,
                    transmitterLocation = "Bremen-Walle",
                    erpKw = "100 kW",
                    category = "Öffentlich-rechtlich (Radio Bremen)",
                    streamUrl = "https://icecast.radiobremen.de/rb/bremenvier/live/mp3/128/stream.mp3",
                    slogan = "Bremen Vier – Hier läuft die Musik",
                    logoBgHex = "#FF6D00"
                ),
                BnetzaStation(
                    id = "bre_roland",
                    name = "Radio Roland",
                    frequencyMhz = 104.8f,
                    transmitterLocation = "Bremen",
                    erpKw = "0.1 kW",
                    category = "Privatfunk",
                    streamUrl = "https://stream.radioroland.de/roland/mp3-128",
                    slogan = "Radio Roland – Echter Schlager für Bremen",
                    logoBgHex = "#D32F2F"
                )
            )
        )
    )

    /**
     * Detailliertes BNetzA-Sendemasten-Kataster mit Koordinaten, ERP (kW) und
     * effektiver Antennenhöhe h_eff (m) für die physikalische ITU-R P.1546 Ausbreitungsberechnung.
     * Enthält alle wesentlichen Grundnetzsender und Lokalsender in NRW und Nachbargebieten.
     */
    private val nrwTransmitterMasterList: List<BnetzaStation> = listOf(
        // --- 1. LOKALFUNK ENNEPE-RUHR & HERDECKE ---
        BnetzaStation(
            id = "en_herdecke_kleff",
            name = "Radio Ennepe Ruhr",
            frequencyMhz = 107.2f,
            transmitterLocation = "Herdecke / Auf dem Kleff",
            erpKw = "0.1 kW",
            category = "Lokalfunk NRW (Ennepe-Ruhr)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6w",
            slogan = "Radio Ennepe Ruhr – Dein bester Mix für Herdecke & EN",
            logoBgHex = "#D32F2F",
            txLatitude = 51.4055,
            txLongitude = 7.4286,
            erpKwDouble = 0.1,
            effectiveHeightMeters = 65.0
        ),
        BnetzaStation(
            id = "en_gevelsberg",
            name = "Radio Ennepe Ruhr",
            frequencyMhz = 91.5f,
            transmitterLocation = "Gevelsberg / Hindenburgturm",
            erpKw = "0.2 kW",
            category = "Lokalfunk NRW (Ennepe-Ruhr)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6w",
            slogan = "Radio Ennepe Ruhr – Die meiste Musik im Kreis",
            logoBgHex = "#D32F2F",
            txLatitude = 51.3267,
            txLongitude = 7.3314,
            erpKwDouble = 0.2,
            effectiveHeightMeters = 110.0
        ),
        BnetzaStation(
            id = "en_witten",
            name = "Radio Ennepe Ruhr",
            frequencyMhz = 104.2f,
            transmitterLocation = "Witten / Stockum",
            erpKw = "0.1 kW",
            category = "Lokalfunk NRW (Witten)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6w",
            slogan = "Radio Ennepe Ruhr 104.2 – Für Witten & Herdecke",
            logoBgHex = "#D32F2F",
            txLatitude = 51.4611,
            txLongitude = 7.3689,
            erpKwDouble = 0.1,
            effectiveHeightMeters = 55.0
        ),

        // --- 2. HAGEN & DORTMUND ---
        BnetzaStation(
            id = "hag_fmt",
            name = "Radio Hagen",
            frequencyMhz = 107.7f,
            transmitterLocation = "Hagen / Fernmeldeturm",
            erpKw = "0.5 kW",
            category = "Lokalfunk NRW (Hagen)",
            streamUrl = "https://stream.lokalradio.nrw/4458h72",
            slogan = "107.7 Radio Hagen – Die beste Musik für Hagen",
            logoBgHex = "#1976D2",
            txLatitude = 51.3536,
            txLongitude = 7.4722,
            erpKwDouble = 0.5,
            effectiveHeightMeters = 160.0
        ),
        BnetzaStation(
            id = "do_florian",
            name = "Radio 91.2",
            frequencyMhz = 91.2f,
            transmitterLocation = "Dortmund / Florianturm",
            erpKw = "0.2 kW",
            category = "Lokalfunk NRW (Dortmund)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6y",
            slogan = "Radio 91.2 – Dortmunds Nummer 1",
            logoBgHex = "#C2185B",
            txLatitude = 51.4967,
            txLongitude = 7.4764,
            erpKwDouble = 0.2,
            effectiveHeightMeters = 175.0
        ),
        BnetzaStation(
            id = "schwerte_wdr2",
            name = "WDR 2 Dortmund",
            frequencyMhz = 87.8f,
            transmitterLocation = "Schwerte / Sommerberg",
            erpKw = "2 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            slogan = "WDR 2 Dortmund – Fürs Ruhrgebiet und Südwestfalen",
            logoBgHex = "#C62828",
            txLatitude = 51.4386,
            txLongitude = 7.5514,
            erpKwDouble = 2.0,
            effectiveHeightMeters = 170.0
        ),

        // --- 3. GRUNDNETZSENDR LANGENBERG (Großsender Ruhr / Bergisches Land) ---
        BnetzaStation(
            id = "lan_wdr5",
            name = "WDR 5",
            frequencyMhz = 88.8f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr5-live.icecast.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3",
            slogan = "WDR 5 – Tiefgang, Reportagen & Information",
            logoBgHex = "#002B49",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_wdr3",
            name = "WDR 3",
            frequencyMhz = 95.1f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr3-live.icecast.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
            slogan = "WDR 3 – Kultur & Klassik",
            logoBgHex = "#5E35B1",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_dlfkultur",
            name = "Deutschlandfunk Kultur",
            frequencyMhz = 96.5f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "50 kW",
            category = "Deutschlandradio",
            streamUrl = "https://st02.sslstream.dlf.de/dlf/02/128/mp3/stream.mp3",
            slogan = "Deutschlandfunk Kultur – Das Feuilleton im Radio",
            logoBgHex = "#4527A0",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 50.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_wdr2",
            name = "WDR 2 Rhein & Ruhr",
            frequencyMhz = 99.2f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            slogan = "WDR 2 – Zusammen sind wir der Westen",
            logoBgHex = "#C62828",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_wdr4",
            name = "WDR 4",
            frequencyMhz = 101.3f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr4-live.icecast.wdr.de/wdr/wdr4/live/mp3/128/stream.mp3",
            slogan = "WDR 4 – Meine Lieblingshits",
            logoBgHex = "#F57C00",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_cosmo",
            name = "COSMO",
            frequencyMhz = 103.3f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-cosmo-live.icecast.wdr.de/wdr/cosmo/live/mp3/128/stream.mp3",
            slogan = "COSMO – Global Sounds & Culture",
            logoBgHex = "#00838F",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),
        BnetzaStation(
            id = "lan_1live",
            name = "1LIVE",
            frequencyMhz = 106.7f,
            transmitterLocation = "Sender Langenberg / Hordtberg",
            erpKw = "100 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            slogan = "1LIVE – Volle Power im Sektor",
            logoBgHex = "#FF8F00",
            txLatitude = 51.3533,
            txLongitude = 7.1350,
            erpKwDouble = 100.0,
            effectiveHeightMeters = 435.0
        ),

        // --- 4. GRUNDNETZSENDR NORDHELLE (Sauerland / Ebbegebirge) ---
        BnetzaStation(
            id = "nor_wdr5",
            name = "WDR 5",
            frequencyMhz = 90.3f,
            transmitterLocation = "Nordhelle / Ebbegebirge",
            erpKw = "35 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr5-live.icecast.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3",
            slogan = "WDR 5 – Radio für Neugierige",
            logoBgHex = "#002B49",
            txLatitude = 51.1486,
            txLongitude = 7.7564,
            erpKwDouble = 35.0,
            effectiveHeightMeters = 610.0
        ),
        BnetzaStation(
            id = "nor_wdr2",
            name = "WDR 2 Südwestfalen",
            frequencyMhz = 93.5f,
            transmitterLocation = "Nordhelle / Ebbegebirge",
            erpKw = "35 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            slogan = "WDR 2 – Immer gut informiert",
            logoBgHex = "#C62828",
            txLatitude = 51.1486,
            txLongitude = 7.7564,
            erpKwDouble = 35.0,
            effectiveHeightMeters = 610.0
        ),
        BnetzaStation(
            id = "nor_wdr3",
            name = "WDR 3",
            frequencyMhz = 98.1f,
            transmitterLocation = "Nordhelle / Ebbegebirge",
            erpKw = "35 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-wdr3-live.icecast.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
            slogan = "WDR 3 – Hören und Fühlen",
            logoBgHex = "#5E35B1",
            txLatitude = 51.1486,
            txLongitude = 7.7564,
            erpKwDouble = 35.0,
            effectiveHeightMeters = 610.0
        ),
        BnetzaStation(
            id = "nor_dlf",
            name = "Deutschlandfunk",
            frequencyMhz = 102.7f,
            transmitterLocation = "Nordhelle / Ebbegebirge",
            erpKw = "20 kW",
            category = "Deutschlandradio",
            streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
            slogan = "Deutschlandfunk – Die Nachrichten im Detail",
            logoBgHex = "#0D47A1",
            txLatitude = 51.1486,
            txLongitude = 7.7564,
            erpKwDouble = 20.0,
            effectiveHeightMeters = 610.0
        ),
        BnetzaStation(
            id = "nor_1live",
            name = "1LIVE",
            frequencyMhz = 104.7f,
            transmitterLocation = "Nordhelle / Ebbegebirge",
            erpKw = "35 kW",
            category = "Öffentlich-rechtlich (WDR)",
            streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            slogan = "1LIVE – Dein Radio im Sektor",
            logoBgHex = "#FF8F00",
            txLatitude = 51.1486,
            txLongitude = 7.7564,
            erpKwDouble = 35.0,
            effectiveHeightMeters = 610.0
        ),

        // --- 5. BOCHUM, ESSEN & RUHR-LOKALFUNK ---
        BnetzaStation(
            id = "bo_ruhr",
            name = "Radio Bochum",
            frequencyMhz = 98.5f,
            transmitterLocation = "Bochum / Fernmeldeturm",
            erpKw = "0.5 kW",
            category = "Lokalfunk NRW (Bochum)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6v",
            slogan = "98.5 Radio Bochum – Das beste Radio für unsere Stadt",
            logoBgHex = "#00897B",
            txLatitude = 51.4883,
            txLongitude = 7.2161,
            erpKwDouble = 0.5,
            effectiveHeightMeters = 150.0
        ),
        BnetzaStation(
            id = "e_essen",
            name = "Radio Essen",
            frequencyMhz = 102.2f,
            transmitterLocation = "Essen / Fernmeldeturm",
            erpKw = "0.3 kW",
            category = "Lokalfunk NRW (Essen)",
            streamUrl = "https://stream.lokalradio.nrw/4458h6x",
            slogan = "Radio Essen 102.2 – Hier ist Essen zuhause",
            logoBgHex = "#3949AB",
            txLatitude = 51.4508,
            txLongitude = 7.0131,
            erpKwDouble = 0.3,
            effectiveHeightMeters = 160.0
        )
    )

    fun getAllRegions(): List<BnetzaRegion> = regions

    fun getRegionById(id: String): BnetzaRegion {
        return regions.firstOrNull { it.id == id } ?: regions[0]
    }

    /**
     * Berechnet mit dem ITU-R P.1546 Ausbreitungsmodell alle tatsächlich empfangbaren UKW-Sender
     * an gegebenen GPS-Koordinaten (z. B. Herdecke: 51.405° N, 7.428° E).
     *
     * Berücksichtigt:
     * - Haversine-Distanz zwischen GPS-Punkt und jedem Sendemast
     * - ITU-R P.1546 Feldstärke E in dBµV/m (unter Berücksichtigung von h_eff und ERP kW)
     * - Filtert alle Sender mit E >= 48 dBµV/m (Mindest-Mono-Empfangsgrenze nach BNetzA)
     * - Ordnet das UKW-Band aufsteigend nach MHz (87.5 - 108.0 MHz)
     */
    fun getStationsForLocation(
        latitude: Double,
        longitude: Double,
        minFieldStrengthDbuV: Double = ItuR1546Engine.THRESHOLD_MONO_DBUV
    ): List<RadioStation> {
        val receivableList = mutableListOf<Pair<BnetzaStation, Double>>()

        // Zuerst alle Sender aus dem präzisen ITU-R Kataster analysieren
        for (station in nrwTransmitterMasterList) {
            val distKm = ItuR1546Engine.calculateDistanceKm(
                latitude, longitude,
                station.txLatitude, station.txLongitude
            )
            val fieldStrength = ItuR1546Engine.calculateFieldStrengthDbuV(
                distKm,
                station.erpKwDouble,
                station.effectiveHeightMeters
            )

            if (fieldStrength >= minFieldStrengthDbuV) {
                receivableList.add(Pair(station, fieldStrength))
            }
        }

        // Falls außerhalb von NRW oder keine Masten empfangbar sind:
        // Als stabiler Fallback die nächstgelegene BNetzA-Region nutzen
        if (receivableList.isEmpty()) {
            val (closestRegion, _) = findClosestRegion(latitude, longitude)
            return closestRegion.stations.map { it.toRadioStation() }
        }

        // Bei überlappenden Frequenzen (gleiche MHz) den Sender mit stärkerer Feldstärke wählen
        val groupedByFreq = receivableList.groupBy { (it.first.frequencyMhz * 10).toInt() }
        val finalStations = groupedByFreq.values.map { listOnFreq ->
            listOnFreq.maxByOrNull { it.second }!!.first
        }

        // Aufsteigend nach UKW-Skala sortieren
        return finalStations.sortedBy { it.frequencyMhz }.map { it.toRadioStation() }
    }

    /**
     * Ermittelt die nächstgelegene Region anhand von GPS-Koordinaten über die Haversine-Formel.
     * Gibt ein Paar aus der Region und der berechneten Distanz in Kilometern zurück.
     */
    fun findClosestRegion(lat: Double, lon: Double): Pair<BnetzaRegion, Double> {
        var closest = regions[0]
        var minDistanceKm = Double.MAX_VALUE

        for (region in regions) {
            val dist = calculateDistanceKm(lat, lon, region.latitude, region.longitude)
            if (dist < minDistanceKm) {
                minDistanceKm = dist
                closest = region
            }
        }
        return Pair(closest, minDistanceKm)
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Erdradius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
