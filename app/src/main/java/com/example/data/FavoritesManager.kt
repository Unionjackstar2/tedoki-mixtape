package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("radio_favorites_prefs", Context.MODE_PRIVATE)

    private val _favoriteStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val favoriteStations: StateFlow<List<RadioStation>> = _favoriteStations.asStateFlow()

    private val _favoriteUuids = MutableStateFlow<Set<String>>(emptySet())
    val favoriteUuids: StateFlow<Set<String>> = _favoriteUuids.asStateFlow()

    private val _limitToTop8InDeck = MutableStateFlow<Boolean>(true)
    val limitToTop8InDeck: StateFlow<Boolean> = _limitToTop8InDeck.asStateFlow()

    init {
        _limitToTop8InDeck.value = prefs.getBoolean(KEY_LIMIT_TO_TOP_8, true)
        loadFavorites()
    }

    private fun loadFavorites() {
        val jsonString = prefs.getString(KEY_FAVORITE_STATIONS_JSON, null)
        if (!jsonString.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<RadioStation>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(stationFromJson(obj))
                }
                _favoriteStations.value = list
                _favoriteUuids.value = list.map { it.stationuuid }.toSet()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Migration or fallback: initialize with curated favorites
        val radioService = RadioBrowserService()
        val defaultFavs = radioService.curatedStations.filter { it.isFavorite }
        val legacyUuids = prefs.getStringSet(KEY_LEGACY_FAVORITES, null)
        val initialList = if (!legacyUuids.isNullOrEmpty()) {
            val matched = radioService.curatedStations.filter { legacyUuids.contains(it.stationuuid) }
            if (matched.isNotEmpty()) matched else defaultFavs
        } else {
            defaultFavs
        }

        saveStations(initialList)
    }

    private fun saveStations(stations: List<RadioStation>) {
        try {
            val jsonArray = JSONArray()
            stations.forEach { station ->
                jsonArray.put(stationToJson(station.copy(isFavorite = true)))
            }
            prefs.edit()
                .putString(KEY_FAVORITE_STATIONS_JSON, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _favoriteStations.value = stations
        _favoriteUuids.value = stations.map { it.stationuuid }.toSet()
    }

    fun isFavorite(uuid: String): Boolean {
        return _favoriteUuids.value.contains(uuid)
    }

    fun setLimitToTop8InDeck(enabled: Boolean) {
        _limitToTop8InDeck.value = enabled
        prefs.edit().putBoolean(KEY_LIMIT_TO_TOP_8, enabled).apply()
    }

    fun toggleFavorite(station: RadioStation): Boolean {
        val currentList = _favoriteStations.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.stationuuid == station.stationuuid }
        val willBeFavorite = existingIndex == -1

        if (willBeFavorite) {
            // Append to favorites with isFavorite = true
            currentList.add(station.copy(isFavorite = true))
        } else {
            currentList.removeAt(existingIndex)
        }

        saveStations(currentList)
        return willBeFavorite
    }

    fun addFavorite(station: RadioStation) {
        val currentList = _favoriteStations.value.toMutableList()
        if (currentList.none { it.stationuuid == station.stationuuid }) {
            currentList.add(station.copy(isFavorite = true))
            saveStations(currentList)
        }
    }

    fun removeFavorite(uuid: String) {
        val currentList = _favoriteStations.value.toMutableList()
        val removed = currentList.removeAll { it.stationuuid == uuid }
        if (removed) {
            saveStations(currentList)
        }
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        val currentList = _favoriteStations.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            saveStations(currentList)
        }
    }

    fun moveFavoriteUp(index: Int) {
        if (index > 0) {
            moveFavorite(index, index - 1)
        }
    }

    fun moveFavoriteDown(index: Int) {
        if (index < _favoriteStations.value.size - 1) {
            moveFavorite(index, index + 1)
        }
    }

    fun updateOrder(newOrder: List<RadioStation>) {
        saveStations(newOrder)
    }

    private fun stationToJson(station: RadioStation): JSONObject {
        return JSONObject().apply {
            put("stationuuid", station.stationuuid)
            put("name", station.name)
            put("streamUrl", station.streamUrl)
            put("favicon", station.favicon)
            put("country", station.country)
            put("language", station.language)
            put("tags", station.tags)
            put("codec", station.codec)
            put("bitrate", station.bitrate)
            put("isFavorite", true)
            put("slogan", station.slogan)
            put("dialFrequencyMhz", station.dialFrequencyMhz.toDouble())
            put("logoBgHex", station.logoBgHex)
            put("logoText", station.logoText)
        }
    }

    private fun stationFromJson(obj: JSONObject): RadioStation {
        val name = obj.optString("name", "Unbekannter Sender")
        return RadioStation(
            stationuuid = obj.optString("stationuuid", "station_${System.currentTimeMillis()}"),
            name = name,
            streamUrl = obj.optString("streamUrl", ""),
            favicon = obj.optString("favicon", ""),
            country = obj.optString("country", "Deutschland"),
            language = obj.optString("language", "Deutsch"),
            tags = obj.optString("tags", ""),
            codec = obj.optString("codec", "MP3"),
            bitrate = obj.optInt("bitrate", 128),
            isFavorite = true,
            slogan = obj.optString("slogan", ""),
            dialFrequencyMhz = obj.optDouble("dialFrequencyMhz", 101.5).toFloat(),
            logoBgHex = obj.optString("logoBgHex", "#" + Integer.toHexString(name.hashCode()).takeLast(6).padStart(6, '3')),
            logoText = obj.optString("logoText", name.take(5))
        )
    }

    companion object {
        private const val KEY_FAVORITE_STATIONS_JSON = "favorite_stations_json_v2"
        private const val KEY_LEGACY_FAVORITES = "favorite_station_uuids"
        private const val KEY_LIMIT_TO_TOP_8 = "limit_to_top_8_in_deck"

        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
