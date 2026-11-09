package de.tedoki.mixtape.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.RecordingMode
import de.tedoki.mixtape.model.TrackMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Cassette::class, TrackMarker::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cassetteDao(): CassetteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tedoki_mixtape_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).cassetteDao()
                                populateInitialData(dao)
                            }
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).cassetteDao()
                                populateInitialData(dao)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(dao: CassetteDao) {
            // Nur in eine leere Datenbank einfuegen – sonst entstehen doppelte Titelmarken
            if (dao.countCassettes() > 0) return
            dao.insertCassette(
                Cassette(
                    id = "tape_mein_mixtape",
                    title = "Mein Mixtape",
                    notes = "Radioaufnahme 1LIVE & WDR",
                    sideA = "Radioaufnahme Roadtrip",
                    sideB = "Hits 2026",
                    colorHex = "#212328",
                    createdAt = System.currentTimeMillis() - 86400000 * 3,
                    radioStationName = "1LIVE",
                    durationSec = 2520, // 42 Min von 60 Min belegt
                    trackCount = 12,
                    recordingMode = RecordingMode.CONTINUOUS,
                    isSample = true,
                    totalCapacitySec = 3600
                )
            )
            dao.insertCassette(
                Cassette(
                    id = "tape_sommer_2026",
                    title = "Sommer 2026",
                    notes = "Urlaubsmusik & Chillout",
                    sideA = "Strand & Sonne",
                    sideB = "Sommerhits",
                    colorHex = "#DCD2BE",
                    createdAt = System.currentTimeMillis() - 86400000 * 2,
                    radioStationName = "SWR3",
                    durationSec = 2160, // 36 Min von 60 Min belegt
                    trackCount = 8,
                    recordingMode = RecordingMode.CONTINUOUS,
                    isSample = true,
                    totalCapacitySec = 3600
                )
            )
            dao.insertCassette(
                Cassette(
                    id = "tape_chill_radio",
                    title = "Chill Radio",
                    notes = "Entspannte Beats für die Nacht",
                    sideA = "Lounge & Lo-Fi",
                    sideB = "Night Session",
                    colorHex = "#2E5B9A",
                    createdAt = System.currentTimeMillis() - 86400000,
                    radioStationName = "radioeins",
                    durationSec = 3360, // 56 Min von 60 Min belegt
                    trackCount = 14,
                    recordingMode = RecordingMode.CLIPS,
                    isSample = true,
                    totalCapacitySec = 3600
                )
            )

            // Seed Track Markers for Mein Mixtape (12 Tracks from 1LIVE)
            val meinMixtapeTracks = listOf(
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Billie Eilish", title = "Lunch", startOffsetSec = 0, endOffsetSec = 180),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Dua Lipa", title = "Houdini", startOffsetSec = 180, endOffsetSec = 370),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Sabrina Carpenter", title = "Espresso", startOffsetSec = 370, endOffsetSec = 545),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "The Weeknd", title = "Blinding Lights", startOffsetSec = 545, endOffsetSec = 745),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Chappell Roan", title = "Good Luck, Babe!", startOffsetSec = 745, endOffsetSec = 960),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Benson Boone", title = "Beautiful Things", startOffsetSec = 960, endOffsetSec = 1140),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Artemas", title = "I Like the Way You Kiss Me", startOffsetSec = 1140, endOffsetSec = 1310),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Teddy Swims", title = "Lose Control", startOffsetSec = 1310, endOffsetSec = 1520),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Taylor Swift", title = "Cruel Summer", startOffsetSec = 1520, endOffsetSec = 1715),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Miley Cyrus", title = "Flowers", startOffsetSec = 1715, endOffsetSec = 1915),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Harry Styles", title = "As It Was", startOffsetSec = 1915, endOffsetSec = 2085),
                TrackMarker(cassetteId = "tape_mein_mixtape", artist = "Coldplay", title = "feelslikeimfallinginlove", startOffsetSec = 2085, endOffsetSec = 2520)
            )
            dao.insertTrackMarkers(meinMixtapeTracks)

            // Seed Track Markers for Sommer 2026 (8 Tracks from SWR3)
            val sommerTracks = listOf(
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Calvin Harris", title = "Summer", startOffsetSec = 0, endOffsetSec = 220),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Kygo & Whitney Houston", title = "Higher Love", startOffsetSec = 220, endOffsetSec = 450),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Robin Schulz", title = "Sugar", startOffsetSec = 450, endOffsetSec = 680),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Lost Frequencies", title = "Are You With Me", startOffsetSec = 680, endOffsetSec = 870),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Felix Jaehn", title = "Ain't Nobody", startOffsetSec = 870, endOffsetSec = 1110),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Avicii", title = "The Nights", startOffsetSec = 1110, endOffsetSec = 1330),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Alvaro Soler", title = "Sofia", startOffsetSec = 1330, endOffsetSec = 1560),
                TrackMarker(cassetteId = "tape_sommer_2026", artist = "Glass Animals", title = "Heat Waves", startOffsetSec = 1560, endOffsetSec = 2160)
            )
            dao.insertTrackMarkers(sommerTracks)

            // Seed Track Markers for Chill Radio (14 Tracks from radioeins)
            val chillTracks = listOf(
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Khruangbin", title = "Texas Sun", startOffsetSec = 0, endOffsetSec = 250),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Tame Impala", title = "Breathe Deeper", startOffsetSec = 250, endOffsetSec = 610),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Cigarettes After Sex", title = "Apocalypse", startOffsetSec = 610, endOffsetSec = 890),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Men I Trust", title = "Show Me How", startOffsetSec = 890, endOffsetSec = 1110),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Mac DeMarco", title = "Chamber of Reflection", startOffsetSec = 1110, endOffsetSec = 1340),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Laufey", title = "From the Start", startOffsetSec = 1340, endOffsetSec = 1510),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "FKJ & Masego", title = "Tadow", startOffsetSec = 1510, endOffsetSec = 1810),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Leon Bridges", title = "Bad Bad News", startOffsetSec = 1810, endOffsetSec = 2020),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Tom Misch", title = "Movie", startOffsetSec = 2020, endOffsetSec = 2360),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Kaytranada", title = "10%", startOffsetSec = 2360, endOffsetSec = 2540),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Norah Jones", title = "Don't Know Why", startOffsetSec = 2540, endOffsetSec = 2730),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Jordan Rakei", title = "Mind's Eye", startOffsetSec = 2730, endOffsetSec = 2940),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Chet Faker", title = "Gold", startOffsetSec = 2940, endOffsetSec = 3190),
                TrackMarker(cassetteId = "tape_chill_radio", artist = "Arlo Parks", title = "Eugene", startOffsetSec = 3190, endOffsetSec = 3360)
            )
            dao.insertTrackMarkers(chillTracks)
        }
    }
}
