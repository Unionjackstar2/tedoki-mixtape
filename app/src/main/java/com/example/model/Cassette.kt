package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecordingMode {
    CONTINUOUS, // Voller Mitschnitt als eine Audiodatei mit Trackmarkern
    CLIPS       // Automatisch in einzelne Clips schneiden
}

enum class PlaybackSource {
    RADIO,
    TAPE
}

enum class DeckState {
    STOPPED,
    PLAYING,
    PAUSED,
    RECORDING,
    RECORDING_PAUSED,
    RECORD_ARMED,
    RECORDING_STARTING,
    RECORDING_STOPPING,
    EJECTED,
    FAST_FORWARDING,
    REWINDING
}

@Entity(tableName = "cassettes")
data class Cassette(
    @PrimaryKey
    val id: String,
    val title: String,
    val notes: String = "",
    val sideA: String = "Seite A",
    val sideB: String = "Seite B",
    val colorHex: String = "#212328", // default dark
    val createdAt: Long = System.currentTimeMillis(),
    val radioStationName: String = "",
    val radioStationUuid: String = "",
    val durationSec: Int = 0,
    val currentPositionSec: Int = 0,
    val filePath: String = "",
    val recordingMode: RecordingMode = RecordingMode.CONTINUOUS,
    val trackCount: Int = 1,
    val isSample: Boolean = false,
    val totalCapacitySec: Int = 3600 // C-60 = 60 minutes = 3600 seconds
)

@Entity(tableName = "track_markers")
data class TrackMarker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cassetteId: String,
    val title: String,
    val artist: String,
    val startOffsetSec: Int,
    val endOffsetSec: Int
)

data class RadioStation(
    val stationuuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String = "",
    val country: String = "Germany",
    val language: String = "German",
    val tags: String = "Pop, Rock",
    val codec: String = "MP3",
    val bitrate: Int = 128,
    val isFavorite: Boolean = false,
    val slogan: String = "",
    val dialFrequencyMhz: Float = 101.5f,
    val logoBgHex: String = "#E91E63",
    val logoText: String = ""
)
