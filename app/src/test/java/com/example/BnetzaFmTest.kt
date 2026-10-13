package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BnetzaFmDatabase
import com.example.data.FmDialMode
import com.example.data.FrequencyTuningManager
import com.example.data.LocationSelectionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BnetzaFmTest {

    private lateinit var context: Context
    private lateinit var manager: FrequencyTuningManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = FrequencyTuningManager.getInstance(context)
    }

    @Test
    fun testBnetzaDatabaseRegionsAndFrequencies() {
        val regions = BnetzaFmDatabase.getAllRegions()
        assertTrue(regions.isNotEmpty())

        val koeln = BnetzaFmDatabase.getRegionById("koeln_bonn")
        assertEquals("Köln / Bonn", koeln.name)
        assertTrue(koeln.stations.isNotEmpty())

        // Prüfen, dass alle Sender im FM-Bereich liegen (87.5 - 108.0 MHz)
        for (station in koeln.stations) {
            assertTrue("Frequenz ${station.frequencyMhz} muss >= 87.5 sein", station.frequencyMhz >= 87.5f)
            assertTrue("Frequenz ${station.frequencyMhz} muss <= 108.0 sein", station.frequencyMhz <= 108.0f)
            assertTrue("Stream URL darf nicht leer sein", station.streamUrl.isNotBlank())
        }

        // Test Haversine-Abstandsberechnung: Koordinaten von Köln (50.938, 6.960)
        val (closest, distanceKm) = BnetzaFmDatabase.findClosestRegion(50.938, 6.960)
        assertEquals("koeln_bonn", closest.id)
        assertTrue(distanceKm < 10.0)

        // Koordinaten von Berlin (52.52, 13.40)
        val (closestBerlin, distBerlin) = BnetzaFmDatabase.findClosestRegion(52.52, 13.40)
        assertEquals("berlin", closestBerlin.id)
        assertTrue(distBerlin < 10.0)
    }

    @Test
    fun testFrequencyTuningManagerModeSwitching() {
        // Standardmodus prüfen
        manager.setFmDialMode(FmDialMode.FAVORITES)
        assertEquals(FmDialMode.FAVORITES, manager.fmDialMode.value)

        // Umschalten auf BNetzA-Ortsmodus
        manager.setFmDialMode(FmDialMode.LOCATION_BNETZA)
        assertEquals(FmDialMode.LOCATION_BNETZA, manager.fmDialMode.value)

        // Region auswählen
        manager.selectRegion("hamburg")
        assertEquals("hamburg", manager.manualRegionId.value)
        assertEquals("Hamburg", manager.activeRegion.value.name)

        val stations = manager.getCurrentBnetzaStations()
        assertTrue(stations.isNotEmpty())
        assertTrue(stations.any { it.name == "Radio Hamburg" || it.name == "NDR 2" })

        // Standortmethode umschalten
        manager.setLocationMethod(LocationSelectionMethod.MANUAL)
        assertEquals(LocationSelectionMethod.MANUAL, manager.locationMethod.value)

        manager.setLocationMethod(LocationSelectionMethod.GPS)
        assertEquals(LocationSelectionMethod.GPS, manager.locationMethod.value)

        // GPS Koordinaten-Update testen
        manager.updateWithGpsCoordinates(48.137, 11.575) // München
        assertEquals("muenchen", manager.gpsRegionId.value)
        assertEquals("München", manager.activeRegion.value.name)
    }

    @Test
    fun testItuRP1546HerdeckeReception() {
        // Koordinaten von Herdecke (Ennepe-Ruhr-Kreis): 51.401 N, 7.433 E
        val herdeckeLat = 51.401
        val herdeckeLon = 7.433

        val receivableStations = BnetzaFmDatabase.getStationsForLocation(herdeckeLat, herdeckeLon)
        assertTrue("In Herdecke müssen Sender empfangbar sein", receivableStations.isNotEmpty())

        // 1. Lokalfunk Ennepe-Ruhr (107.2 MHz vom Kleff) muss empfangbar sein
        val radioEn = receivableStations.firstOrNull { it.dialFrequencyMhz == 107.2f }
        assertNotNull("Radio Ennepe Ruhr (107.2 MHz vom Kleff) muss in Herdecke empfangbar sein", radioEn)
        assertEquals("Radio Ennepe Ruhr", radioEn?.name)

        // 2. Radio Hagen (107.7 MHz vom Fernmeldeturm Hagen) muss empfangbar sein
        val radioHagen = receivableStations.firstOrNull { it.dialFrequencyMhz == 107.7f }
        assertNotNull("Radio Hagen (107.7 MHz) muss in Herdecke empfangbar sein", radioHagen)

        // 3. WDR Langenberg Großsender (z.B. 1LIVE 106.7 MHz, WDR 2 99.2 MHz, WDR 5 88.8 MHz)
        val wdrLangenberg = receivableStations.filter {
            it.dialFrequencyMhz in listOf(88.8f, 95.1f, 96.5f, 99.2f, 101.3f, 103.3f, 106.7f)
        }
        assertTrue("WDR Langenberg Großsender muss in Herdecke empfangbar sein", wdrLangenberg.isNotEmpty())

        // 4. Prüfen, dass Frequenzen aufsteigend nach UKW-Skala sortiert sind
        for (i in 0 until receivableStations.size - 1) {
            assertTrue(
                "Frequenzen müssen aufsteigend sortiert sein",
                receivableStations[i].dialFrequencyMhz <= receivableStations[i + 1].dialFrequencyMhz
            )
        }
    }

    @Test
    fun testModeSwitchingAndStationPoolIntegrity() {
        // 1. Start in GPS-Modus mit Herdecke-Koordinaten
        manager.setFmDialMode(FmDialMode.LOCATION_BNETZA)
        manager.setLocationMethod(LocationSelectionMethod.GPS)
        manager.updateWithGpsCoordinates(51.401, 7.433)
        val gpsStations = manager.getCurrentBnetzaStations()
        assertTrue(gpsStations.any { it.name == "Radio Ennepe Ruhr" })

        // 2. Wechsel auf MANUELL und Auswahl von Hamburg
        manager.setLocationMethod(LocationSelectionMethod.MANUAL)
        manager.selectRegion("hamburg")
        assertEquals(LocationSelectionMethod.MANUAL, manager.locationMethod.value)
        assertEquals("hamburg", manager.manualRegionId.value)
        assertEquals("Hamburg", manager.activeRegion.value.name)

        // Der Pool darf JETZT NICHT mehr GPS/Herdecke-Sender enthalten, sondern Hamburg!
        val manualStations = manager.getCurrentBnetzaStations()
        assertTrue(manualStations.any { it.name == "Radio Hamburg" || it.name == "NDR 2" })
        assertFalse(manualStations.any { it.name == "Radio Ennepe Ruhr" })

        // 3. Wechsel auf FAVORITEN
        manager.setFmDialMode(FmDialMode.FAVORITES)
        assertEquals(FmDialMode.FAVORITES, manager.fmDialMode.value)
    }
}
