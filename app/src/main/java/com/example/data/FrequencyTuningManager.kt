package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class FmDialMode {
    FAVORITES,          // Favoriten auf dem Band abbilden (bisheriges Verhalten)
    LOCATION_BNETZA     // Echte FM-Radiosender nach Ort/GPS abbilden (BNetzA Open Data)
}

enum class LocationSelectionMethod {
    GPS,                // Automatische Ortung per GPS / Netzwerk
    MANUAL              // Manueller Aufenthaltsort aus BNetzA-Städten
}

class FrequencyTuningManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        "pref_frequency_tuning",
        Context.MODE_PRIVATE
    )

    private val _fmDialMode = MutableStateFlow(loadFmDialMode())
    val fmDialMode: StateFlow<FmDialMode> = _fmDialMode.asStateFlow()

    private val _locationMethod = MutableStateFlow(loadLocationMethod())
    val locationMethod: StateFlow<LocationSelectionMethod> = _locationMethod.asStateFlow()

    // Getrennte Speicherung für GPS-Region und manuell gewählte Region
    private val _manualRegionId = MutableStateFlow(loadManualRegionId())
    val manualRegionId: StateFlow<String> = _manualRegionId.asStateFlow()

    private val _gpsRegionId = MutableStateFlow(loadGpsRegionId())
    val gpsRegionId: StateFlow<String> = _gpsRegionId.asStateFlow()

    // Die tatsächlich aktive Region: Richtet sich strikt nach der gewählten locationMethod
    private val _activeRegion = MutableStateFlow(resolveActiveRegion(_locationMethod.value, _manualRegionId.value, _gpsRegionId.value))
    val activeRegion: StateFlow<BnetzaRegion> = _activeRegion.asStateFlow()

    private val _gpsLocationInfo = MutableStateFlow(
        prefs.getString("pref_last_gps_info", "GPS-Ortung bereit (auf Knopfdruck)") ?: "GPS-Ortung bereit (auf Knopfdruck)"
    )
    val gpsLocationInfo: StateFlow<String> = _gpsLocationInfo.asStateFlow()

    private val _lastGpsCoords = MutableStateFlow<Pair<Double, Double>?>(
        if (prefs.contains("pref_last_gps_lat") && prefs.contains("pref_last_gps_lon")) {
            Pair(
                prefs.getFloat("pref_last_gps_lat", 0f).toDouble(),
                prefs.getFloat("pref_last_gps_lon", 0f).toDouble()
            )
        } else null
    )

    private val _isFmMutingEnabled = MutableStateFlow(
        prefs.getBoolean("pref_fm_muting_enabled", false) // Standard: Authentisches Rauschen aktiv (Muting aus)
    )
    val isFmMutingEnabled: StateFlow<Boolean> = _isFmMutingEnabled.asStateFlow()

    private val _isGpsLocating = MutableStateFlow(false)
    val isGpsLocating: StateFlow<Boolean> = _isGpsLocating.asStateFlow()

    // Referenz auf aktiven LocationListener für sauberen Abbruch
    private var activeLocationListener: LocationListener? = null

    companion object {
        @Volatile
        private var instance: FrequencyTuningManager? = null

        fun getInstance(context: Context): FrequencyTuningManager {
            return instance ?: synchronized(this) {
                instance ?: FrequencyTuningManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun loadFmDialMode(): FmDialMode {
        val raw = prefs.getString("fm_dial_mode", FmDialMode.FAVORITES.name)
        return try {
            FmDialMode.valueOf(raw ?: FmDialMode.FAVORITES.name)
        } catch (e: Exception) {
            FmDialMode.FAVORITES
        }
    }

    private fun loadLocationMethod(): LocationSelectionMethod {
        val raw = prefs.getString("location_selection_method", LocationSelectionMethod.MANUAL.name)
        return try {
            LocationSelectionMethod.valueOf(raw ?: LocationSelectionMethod.MANUAL.name)
        } catch (e: Exception) {
            LocationSelectionMethod.MANUAL
        }
    }

    private fun loadManualRegionId(): String {
        return prefs.getString("manual_region_id", prefs.getString("selected_region_id", "koeln_bonn") ?: "koeln_bonn") ?: "koeln_bonn"
    }

    private fun loadGpsRegionId(): String {
        return prefs.getString("gps_region_id", "koeln_bonn") ?: "koeln_bonn"
    }

    private fun resolveActiveRegion(
        method: LocationSelectionMethod,
        manualId: String,
        gpsId: String
    ): BnetzaRegion {
        val targetId = if (method == LocationSelectionMethod.GPS) gpsId else manualId
        return BnetzaFmDatabase.getRegionById(targetId)
    }

    fun setFmDialMode(mode: FmDialMode) {
        _fmDialMode.value = mode
        prefs.edit().putString("fm_dial_mode", mode.name).apply()
    }

    fun setFmMutingEnabled(enabled: Boolean) {
        _isFmMutingEnabled.value = enabled
        prefs.edit().putBoolean("pref_fm_muting_enabled", enabled).apply()
    }

    /**
     * Schaltet zwischen GPS-Ortung und manueller Ortswahl um.
     * Bricht eventuell laufende GPS-Listener sofort ab und übernimmt zuverlässig die jeweilige Region.
     */
    fun setLocationMethod(method: LocationSelectionMethod) {
        if (method == LocationSelectionMethod.MANUAL) {
            cancelPendingGpsUpdates()
        }

        _locationMethod.value = method
        prefs.edit().putString("location_selection_method", method.name).apply()

        // Sofort die korrekte Zielregion als aktiv setzen
        val newActive = resolveActiveRegion(method, _manualRegionId.value, _gpsRegionId.value)
        _activeRegion.value = newActive
    }

    /**
     * Wählt explizit eine manuelle Region aus und aktiviert sie sofort.
     */
    fun selectRegion(regionId: String) {
        selectManualRegion(regionId)
    }

    fun selectManualRegion(regionId: String) {
        cancelPendingGpsUpdates()

        val region = BnetzaFmDatabase.getRegionById(regionId)
        _manualRegionId.value = region.id
        prefs.edit().putString("manual_region_id", region.id).apply()

        // Auf MANUAL umschalten und sofort als aktiv setzen
        _locationMethod.value = LocationSelectionMethod.MANUAL
        prefs.edit().putString("location_selection_method", LocationSelectionMethod.MANUAL.name).apply()

        _activeRegion.value = region
    }

    /**
     * Verarbeitet GPS-Koordinaten. Aktualisiert die GPS-Region und setzt diese nur dann als aktiv,
     * wenn der Nutzer sich aktuell im GPS-Modus befindet. Überschreibt niemals manuelle Regionen!
     */
    fun updateWithGpsCoordinates(latitude: Double, longitude: Double) {
        _lastGpsCoords.value = Pair(latitude, longitude)
        prefs.edit()
            .putFloat("pref_last_gps_lat", latitude.toFloat())
            .putFloat("pref_last_gps_lon", longitude.toFloat())
            .apply()

        // ITU-R P.1546 Ausbreitungsprognose für die genauen Koordinaten berechnen
        val ituStations = BnetzaFmDatabase.getStationsForLocation(latitude, longitude)
        val (closestRegion, distanceKm) = BnetzaFmDatabase.findClosestRegion(latitude, longitude)

        val infoText = String.format(
            Locale.GERMAN,
            "%.3f° N, %.3f° E • %d Sender empfangbar (ITU-R P.1546)",
            latitude,
            longitude,
            ituStations.size
        )
        _gpsLocationInfo.value = infoText
        prefs.edit().putString("pref_last_gps_info", infoText).apply()

        _gpsRegionId.value = closestRegion.id
        prefs.edit().putString("gps_region_id", closestRegion.id).apply()

        // Wenn GPS-Modus aktiv ist, aktive Region aktualisieren
        if (_locationMethod.value == LocationSelectionMethod.GPS) {
            _activeRegion.value = closestRegion
        }
    }

    /**
     * Bricht jede laufende GPS-Abfrage sauber ab.
     */
    fun cancelPendingGpsUpdates() {
        _isGpsLocating.value = false
        val listener = activeLocationListener
        if (listener != null) {
            try {
                val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                lm?.removeUpdates(listener)
            } catch (ignored: Exception) {
            }
            activeLocationListener = null
        }
    }

    /**
     * Führt eine einmalige GPS-Ortung ausschließlich auf Abruf (Knopfdruck) durch.
     */
    @SuppressLint("MissingPermission")
    fun performGpsLocation(context: Context, onComplete: (Boolean, String) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            val msg = "Ortungsdienst nicht verfügbar"
            _gpsLocationInfo.value = msg
            onComplete(false, msg)
            return
        }

        cancelPendingGpsUpdates()
        _isGpsLocating.value = true

        try {
            // Zuerst letzten bekannten Standort prüfen (GPS, Network, Passive)
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            }

            // Wenn frischer letzter Standort vorhanden ist (< 15 Minuten)
            val now = System.currentTimeMillis()
            if (bestLocation != null && (now - bestLocation.time) < 15 * 60 * 1000L) {
                updateWithGpsCoordinates(bestLocation.latitude, bestLocation.longitude)
                _isGpsLocating.value = false
                val msg = "Standort ermittelt: ${_activeRegion.value.name}"
                onComplete(true, msg)
                return
            }

            // Einmaliges Update anfordern
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (ignored: Exception) {
                    }
                    activeLocationListener = null
                    _isGpsLocating.value = false
                    updateWithGpsCoordinates(location.latitude, location.longitude)
                    onComplete(true, "GPS-Ortung aktualisiert: ${_activeRegion.value.name}")
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            activeLocationListener = listener

            val availableProvider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                null
            }

            if (availableProvider != null) {
                locationManager.requestSingleUpdate(availableProvider, listener, Looper.getMainLooper())
            } else if (bestLocation != null) {
                // Fallback auf älteren Standort, falls kein Live-Provider aktiv ist
                updateWithGpsCoordinates(bestLocation.latitude, bestLocation.longitude)
                _isGpsLocating.value = false
                onComplete(true, "Letzter Standort übernommen: ${_activeRegion.value.name}")
            } else {
                _isGpsLocating.value = false
                val msg = "GPS/Standort auf dem Gerät deaktiviert"
                _gpsLocationInfo.value = msg
                onComplete(false, msg)
            }
        } catch (e: SecurityException) {
            _isGpsLocating.value = false
            val msg = "Standortberechtigung erforderlich"
            _gpsLocationInfo.value = msg
            onComplete(false, msg)
        } catch (e: Exception) {
            _isGpsLocating.value = false
            val msg = "Ortungsfehler: ${e.message}"
            _gpsLocationInfo.value = msg
            onComplete(false, msg)
        }
    }

    /**
     * Gibt die BNetzA-Radiostationen zurück.
     * Im GPS-Modus wird die physikalische ITU-R P.1546 Ausbreitungsprognose für die
     * exakten Koordinaten berechnet. Im manuellen Modus wird die gewählte Region zurückgegeben.
     */
    fun getCurrentBnetzaStations(): List<RadioStation> {
        if (_locationMethod.value == LocationSelectionMethod.GPS) {
            val coords = _lastGpsCoords.value
            if (coords != null) {
                return BnetzaFmDatabase.getStationsForLocation(coords.first, coords.second)
            }
        }
        return _activeRegion.value.stations.map { it.toRadioStation() }
    }
}
