package de.tedoki.mixtape

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.tedoki.mixtape.audio.AudioEngine
import de.tedoki.mixtape.data.AppDatabase
import de.tedoki.mixtape.data.FavoritesManager
import de.tedoki.mixtape.data.RadioBrowserService
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.DeckState
import de.tedoki.mixtape.model.PlaybackSource
import de.tedoki.mixtape.model.RadioStation
import de.tedoki.mixtape.ui.screens.CassetteCustomizerScreen
import de.tedoki.mixtape.ui.screens.CassetteDeckScreen
import de.tedoki.mixtape.ui.screens.CassetteDetailScreen
import de.tedoki.mixtape.ui.screens.CassetteShelfScreen
import de.tedoki.mixtape.ui.screens.StationBrowserScreen
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark
import de.tedoki.mixtape.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    DECK,
    STATIONS,
    SHELF,
    DETAIL,
    CUSTOMIZER
}

class MainActivity : ComponentActivity() {
    companion object {
        var sharedAudioEngine: AudioEngine? = null
    }

    private var audioEngine: AudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_deck_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("pref_fullscreen_enabled", false)) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val engine = sharedAudioEngine ?: AudioEngine(applicationContext).also {
            sharedAudioEngine = it
        }
        audioEngine = engine
        val database = AppDatabase.getDatabase(applicationContext)
        val radioService = RadioBrowserService()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeckSurfaceDark
                ) {
                    MixtapeApp(
                        audioEngine = engine,
                        database = database,
                        radioService = radioService
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val engine = audioEngine
        // Nur freigeben, wenn weder Kassette noch Radio noch eine Aufnahme laeuft –
        // sonst wuerde das Radio beim Zurueck-Wischen aus der App abbrechen.
        val busy = engine != null && (
            engine.isRadioPlaying.value ||
            engine.deckState.value == DeckState.PLAYING ||
            engine.deckState.value == DeckState.RECORDING ||
            engine.deckState.value == DeckState.RECORDING_PAUSED ||
            engine.deckState.value == DeckState.RECORDING_STARTING ||
            engine.deckState.value == DeckState.RECORDING_STOPPING
        )
        if (isFinishing && engine != null && !busy) {
            engine.release()
            sharedAudioEngine = null
        }
    }
}

@Composable
fun MixtapeApp(
    audioEngine: AudioEngine,
    database: AppDatabase,
    radioService: RadioBrowserService
) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* permission result handled */ }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.DECK) }
    var editingTape by remember { mutableStateOf<Cassette?>(null) }
    var viewingTapeForDetail by remember { mutableStateOf<Cassette?>(null) }

    val cassettes by database.cassetteDao().getAllCassettes().collectAsState(initial = emptyList())
    val allTrackMarkers by database.cassetteDao().getAllTrackMarkers().collectAsState(initial = emptyList())
    val tracksByCassette = remember(allTrackMarkers) { allTrackMarkers.groupBy { it.cassetteId } }
    val favoritesManager = remember { FavoritesManager.getInstance(context) }
    val favoriteStations by favoritesManager.favoriteStations.collectAsState()
    val stations = remember(favoriteStations, radioService.curatedStations) {
        val combined = (favoriteStations + radioService.curatedStations).distinctBy { it.stationuuid }
        if (combined.isNotEmpty()) combined else radioService.curatedStations
    }

    // Initialize initial station
    // (Die Beispielkassetten legt die Datenbank selbst beim ersten Anlegen an.
    //  Frueher wurde hier bei jedem Start erneut befuellt -> doppelte Titel auf den Demo-Kassetten.)
    LaunchedEffect(Unit) {
        if (stations.isNotEmpty() && audioEngine.currentStation.value == null) {
            val initialStation = favoriteStations.firstOrNull() ?: stations[0]
            audioEngine.setStation(initialStation)
        }
    }

    // Systemtaste "Zurueck": innerhalb der App zurueck statt die App zu schliessen
    BackHandler(enabled = currentScreen != AppScreen.DECK) {
        currentScreen = when (currentScreen) {
            AppScreen.DETAIL -> AppScreen.SHELF
            AppScreen.CUSTOMIZER -> if (viewingTapeForDetail != null) AppScreen.DETAIL else AppScreen.DECK
            else -> AppScreen.DECK
        }
    }

    LaunchedEffect(cassettes) {
        if (cassettes.isNotEmpty()) {
            val current = audioEngine.activeTape.value
            if (current == null) {
                audioEngine.setActiveTape(cassettes.first())
            } else {
                val dbMatch = cassettes.find { it.id == current.id }
                if (dbMatch != null && dbMatch != current) {
                    val isDeckBusy = audioEngine.deckState.value == DeckState.PLAYING ||
                            audioEngine.deckState.value == DeckState.RECORDING ||
                            audioEngine.deckState.value == DeckState.RECORDING_STARTING ||
                            audioEngine.deckState.value == DeckState.RECORDING_STOPPING
                    if (!isDeckBusy) {
                        audioEngine.setActiveTape(dbMatch)
                    }
                }
            }
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.DECK -> {
                CassetteDeckScreen(
                    audioEngine = audioEngine,
                    stations = stations,
                    onOpenStations = { currentScreen = AppScreen.STATIONS },
                    onOpenShelf = { currentScreen = AppScreen.SHELF },
                    onOpenCustomizer = { tape ->
                        editingTape = tape
                        currentScreen = AppScreen.CUSTOMIZER
                    },
                    onSaveCassette = { newTape, markers ->
                        scope.launch {
                            database.cassetteDao().insertCassette(newTape)
                            if (markers.isNotEmpty()) {
                                database.cassetteDao().insertTrackMarkers(markers)
                            }
                        }
                    },
                    onOpenDetail = { tape ->
                        viewingTapeForDetail = tape
                        currentScreen = AppScreen.DETAIL
                    }
                )
            }

            AppScreen.STATIONS -> {
                StationBrowserScreen(
                    currentStation = audioEngine.currentStation.collectAsState().value,
                    isRadioPlaying = audioEngine.isRadioPlaying.collectAsState().value,
                    nowPlayingText = audioEngine.nowPlayingTitle.collectAsState().value,
                    onTogglePlayback = { audioEngine.toggleRadioPlayback() },
                    onStationSelect = { selectedStation ->
                        audioEngine.playStation(selectedStation)
                        currentScreen = AppScreen.DECK
                    },
                    onBack = { currentScreen = AppScreen.DECK }
                )
            }

            AppScreen.SHELF -> {
                CassetteShelfScreen(
                    cassettes = cassettes,
                    allTracks = tracksByCassette,
                    onSelectTape = { tape ->
                        viewingTapeForDetail = tape
                        currentScreen = AppScreen.DETAIL
                    },
                    onCreateNewTape = {
                        val newTape = Cassette(
                            id = "tape_${System.currentTimeMillis()}",
                            title = "Neues Mixtape",
                            notes = "Erstellt am Deck",
                            sideA = "Seite A",
                            sideB = "Seite B",
                            colorHex = "#D86228"
                        )
                        editingTape = newTape
                        currentScreen = AppScreen.CUSTOMIZER
                    },
                    onBack = { currentScreen = AppScreen.DECK }
                )
            }

            AppScreen.DETAIL -> {
                val tape = viewingTapeForDetail ?: cassettes.firstOrNull() ?: Cassette(
                    id = "tape_temp",
                    title = "Mixtape",
                    colorHex = "#212328"
                )
                CassetteDetailScreen(
                    cassette = tape,
                    tracks = tracksByCassette[tape.id] ?: emptyList(),
                    onPlayInDeck = { tapeToPlay ->
                        audioEngine.insertTape(tapeToPlay)
                        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
                        currentScreen = AppScreen.DECK
                    },
                    onEditTape = { tapeToEdit ->
                        editingTape = tapeToEdit
                        currentScreen = AppScreen.CUSTOMIZER
                    },
                    onDeleteTape = { tapeToDelete ->
                        val isActive = audioEngine.activeTape.value?.id == tapeToDelete.id
                        // Nur eigene Aufnahmen im App-Ordner "recordings" loeschen, nie fremde Dateien
                        val recordingsDir = java.io.File(context.filesDir, "recordings").canonicalPath
                        val audioFile = if (!tapeToDelete.isSample && !isActive) {
                            audioEngine.resolveTapeAudioFile(tapeToDelete)
                                ?.takeIf { it.canonicalPath.startsWith(recordingsDir) }
                        } else null
                        scope.launch {
                            database.cassetteDao().deleteCassette(tapeToDelete)
                            // Titelmarken und Aufnahmedatei mit entfernen (sonst bleiben Datenleichen)
                            database.cassetteDao().deleteTrackMarkersForCassette(tapeToDelete.id)
                            withContext(Dispatchers.IO) {
                                try { audioFile?.takeIf { it.exists() }?.delete() } catch (e: Exception) { }
                            }
                        }
                        if (viewingTapeForDetail?.id == tapeToDelete.id) viewingTapeForDetail = null
                        currentScreen = AppScreen.SHELF
                    },
                    onBack = { currentScreen = AppScreen.SHELF }
                )
            }

            AppScreen.CUSTOMIZER -> {
                val tapeToEdit = editingTape ?: Cassette(
                    id = "tape_${System.currentTimeMillis()}",
                    title = "Mein Mixtape",
                    notes = "",
                    colorHex = "#212328"
                )

                CassetteCustomizerScreen(
                    initialCassette = tapeToEdit,
                    onSave = { updatedTape ->
                        scope.launch {
                            database.cassetteDao().insertCassette(updatedTape)
                            if (audioEngine.activeTape.value?.id == updatedTape.id) {
                                audioEngine.setActiveTape(updatedTape)
                            }
                        }
                        if (viewingTapeForDetail?.id == updatedTape.id) {
                            viewingTapeForDetail = updatedTape
                        }
                    },
                    onBack = {
                        if (viewingTapeForDetail != null) {
                            currentScreen = AppScreen.DETAIL
                        } else {
                            currentScreen = AppScreen.DECK
                        }
                    }
                )
            }
        }
    }
}
