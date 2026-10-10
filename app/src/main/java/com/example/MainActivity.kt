package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.audio.AudioEngine
import com.example.data.AppDatabase
import com.example.data.RadioBrowserService
import com.example.model.Cassette
import com.example.model.DeckState
import com.example.model.PlaybackSource
import com.example.model.RadioStation
import com.example.ui.screens.CassetteCustomizerScreen
import com.example.ui.screens.CassetteDeckScreen
import com.example.ui.screens.CassetteDetailScreen
import com.example.ui.screens.CassetteShelfScreen
import com.example.ui.screens.StationBrowserScreen
import com.example.ui.theme.DeckSurfaceDark
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

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
        if (isFinishing && engine != null && engine.deckState.value != DeckState.PLAYING) {
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
    val stations = remember { radioService.curatedStations }

    // Initialize initial station and tape
    LaunchedEffect(Unit) {
        if (stations.isNotEmpty() && audioEngine.currentStation.value == null) {
            audioEngine.setStation(stations[0]) // 1LIVE
        }
        scope.launch {
            if (cassettes.isEmpty() || allTrackMarkers.isEmpty()) {
                AppDatabase.populateInitialData(database.cassetteDao())
            }
        }
    }

    LaunchedEffect(cassettes) {
        if (cassettes.isNotEmpty() && audioEngine.activeTape.value == null) {
            audioEngine.setActiveTape(cassettes.first())
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
                    }
                )
            }

            AppScreen.STATIONS -> {
                StationBrowserScreen(
                    currentStation = audioEngine.currentStation.collectAsState().value,
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
                        scope.launch {
                            database.cassetteDao().deleteCassette(tapeToDelete)
                        }
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
