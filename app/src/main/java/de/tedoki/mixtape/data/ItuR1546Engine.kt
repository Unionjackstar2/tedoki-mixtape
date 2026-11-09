package de.tedoki.mixtape.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mathematische Näherung der ITU-R P.1546 Empfangsprognose für VHF Band II (100 MHz UKW-Rundfunk).
 *
 * Berechnet die am Empfängerort eintreffende Feldstärke E in dBµV/m (50% Zeit, 50% Ort)
 * basierend auf:
 * - Distanz d (km) zwischen Sender und Empfänger
 * - Effektiver Sendeantennenhöhe h_eff (m) über mittlerem Gelände
 * - Effektiver Strahlungsleistung ERP (kW bzw. dBkW)
 */
object ItuR1546Engine {

    // ITU-R / BNetzA Mindest-Feldstärkeschwellen für UKW-FM Band II
    const val THRESHOLD_STEREO_DBUV = 54.0 // Einwandfreier Stereo-Empfang (High Quality)
    const val THRESHOLD_MONO_DBUV = 48.0   // Mindestversorgung / brauchbarer Mono-Empfang

    /**
     * Berechnet die Feldstärke E in dBµV/m für eine gegebene Sendeanlage.
     *
     * @param distanceKm Entfernung in Kilometern
     * @param erpKw Effektive Strahlungsleistung in Kilowatt (ERP)
     * @param effectiveHeightMeters Effektive Antennenhöhe h_eff in Metern über dem mittleren Gelände
     * @return Berechnete Feldstärke in dBµV/m
     */
    fun calculateFieldStrengthDbuV(
        distanceKm: Double,
        erpKw: Double,
        effectiveHeightMeters: Double
    ): Double {
        val d = max(0.2, distanceKm)
        val h = max(10.0, effectiveHeightMeters)
        val pErp = max(0.001, erpKw)

        // 1. Logarithmische Sendeleistung ERP bezogen auf 1 kW: P_dBkW = 10 * log10(ERP_kW)
        val pDbKw = 10.0 * log10(pErp)

        // 2. Referenzfeldstärke E_ref(d, h) für 1 kW ERP bei 100 MHz nach ITU-R P.1546
        // Für Entfernungen im optischen Nahbereich (Freiraumdämpfung + Erdbodeneinfluss)
        // bis hin zur Beugung über den Radiohorizont:
        // Optischer Radiohorizont d_h ≈ 4.12 * (sqrt(h_tx) + sqrt(h_rx_2m))
        val radioHorizonKm = 4.12 * (sqrt(h) + sqrt(2.0))

        val eRef: Double = if (d <= radioHorizonKm) {
            // Innerhalb des Radiohorizonts (quasi-optische Sicht / quasi-freie Ausbreitung)
            // Basis: 106.9 dBµV/m bei 1 km für 1 kW
            val pathLossExponent = 20.0 + 8.0 * (d / radioHorizonKm)
            val baseLoss = pathLossExponent * log10(d)
            val heightGain = 20.0 * log10(h / 37.5) // bezogen auf Referenzhöhe 37.5m
            106.9 - baseLoss + heightGain * (1.0 - (d / (2.5 * radioHorizonKm)))
        } else {
            // Hinter dem Radiohorizont: Beugung an Erdkrümmung / Diffraktionsbereich
            val horizonLoss = 28.0 * log10(radioHorizonKm)
            val heightGain = 20.0 * log10(h / 37.5)
            val eAtHorizon = 106.9 - horizonLoss + heightGain * 0.6
            val diffractionSlope = 0.85 // ca. 0.85 dB/km zusätzliche Dämpfung im Beugungsbereich bei 100 MHz
            eAtHorizon - (d - radioHorizonKm) * diffractionSlope
        }

        // Gesamtfeldstärke = Referenzfeldstärke (für 1 kW) + Leistungspegel des Senders
        return eRef + pDbKw
    }

    /**
     * Prüft, ob ein Sender am Empfangsort die Mindestfeldstärke (Mono oder Stereo) erreicht.
     */
    fun isReceivable(
        distanceKm: Double,
        erpKw: Double,
        effectiveHeightMeters: Double,
        minThresholdDbuV: Double = THRESHOLD_MONO_DBUV
    ): Boolean {
        return calculateFieldStrengthDbuV(distanceKm, erpKw, effectiveHeightMeters) >= minThresholdDbuV
    }

    /**
     * Berechnet die Distanz zwischen zwei Koordinaten in Kilometern (Haversine-Formel).
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Mittlerer Erdradius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
