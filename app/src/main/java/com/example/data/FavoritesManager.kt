package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("radio_favorites_prefs", Context.MODE_PRIVATE)

    private val _favoriteUuids = MutableStateFlow<Set<String>>(emptySet())
    val favoriteUuids: StateFlow<Set<String>> = _favoriteUuids.asStateFlow()

    init {
        val stored = prefs.getStringSet(KEY_FAVORITES, null)
        if (stored != null) {
            _favoriteUuids.value = stored.toSet()
        } else {
            // Default initial favorites matching screenshot
            val initial = setOf("curated_1live", "curated_wdr2", "curated_radioeins")
            prefs.edit().putStringSet(KEY_FAVORITES, initial).apply()
            _favoriteUuids.value = initial
        }
    }

    fun isFavorite(uuid: String): Boolean {
        return _favoriteUuids.value.contains(uuid)
    }

    fun toggleFavorite(uuid: String): Boolean {
        val current = _favoriteUuids.value.toMutableSet()
        val willBeFavorite = !current.contains(uuid)
        if (willBeFavorite) {
            current.add(uuid)
        } else {
            current.remove(uuid)
        }
        val immutableCopy = HashSet(current)
        prefs.edit().putStringSet(KEY_FAVORITES, immutableCopy).commit()
        _favoriteUuids.value = immutableCopy
        return willBeFavorite
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_station_uuids"

        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
