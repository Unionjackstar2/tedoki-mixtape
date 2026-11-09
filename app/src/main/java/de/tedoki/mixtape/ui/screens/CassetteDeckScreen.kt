package de.tedoki.mixtape.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.audio.AudioEngine
import de.tedoki.mixtape.data.FavoritesManager
import de.tedoki.mixtape.data.FmDialMode
import de.tedoki.mixtape.data.FrequencyTuningManager
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.DeckState
import de.tedoki.mixtape.model.PlaybackSource
import de.tedoki.mixtape.model.RadioLayoutMode
import de.tedoki.mixtape.model.RadioStation
import de.tedoki.mixtape.model.RecordingMode
import de.tedoki.mixtape.model.TrackMarker
import de.tedoki.mixtape.ui.components.CassetteDoorCompartment
import de.tedoki.mixtape.ui.components.CassetteView
import de.tedoki.mixtape.ui.components.DigitalRadioPanel
import de.tedoki.mixtape.ui.components.DualVuMeter
import de.tedoki.mixtape.ui.components.FrequencySlider
import de.tedoki.mixtape.ui.components.FunctionSelectorSwitch
import de.tedoki.mixtape.ui.components.MechanicalTapeCounter
import de.tedoki.mixtape.ui.components.RecordingOptionsSheet
import de.tedoki.mixtape.ui.components.SettingsSheet
import de.tedoki.mixtape.ui.components.TransportControls
import de.tedoki.mixtape.ui.components.TuningScale
import de.tedoki.mixtape.ui.components.VuMeterAndCounter
import de.tedoki.mixtape.ui.theme.DeckAnthracite
import de.tedoki.mixtape.ui.theme.DeckChassisBevel
import de.tedoki.mixtape.ui.theme.DeckChassisHighlight
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark

@Composable
fun CassetteDeckScreen(
    audioEngine: AudioEngine,
    stations: List<RadioStation>,
    onOpenStations: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenCustomizer: (Cassette) -> Unit,
    onSaveCassette: (Cassette, List<TrackMarker>) -> Unit,
    onOpenDetail: ((Cassette) -> Unit)? = null
) {
    val context = LocalContext.current
    val deckState by audioEngine.deckState.collectAsState()
    val activeTape by audioEngine.activeTape.collectAsState()
    val currentStation by audioEngine.currentStation.collectAsState()
    val nowPlaying by audioEngine.nowPlayingTitle.collectAsState()
    val vuLeft by audioEngine.vuLeft.collectAsState()
    val vuRight by audioEngine.vuRight.collectAsState()
    val counter by audioEngine.tapeCounter.collectAsState()
    val tapePositionSec by audioEngine.tapePositionSec.collectAsState()
    val playbackSource by audioEngine.playbackSource.collectAsState()
    val isRadioPlaying by audioEngine.isRadioPlaying.collectAsState()

    val favoritesManager = remember { FavoritesManager.getInstance(context) }
    val favoriteStations by favoritesManager.favoriteStations.collectAsState()
    val favoriteUuids by favoritesManager.favoriteUuids.collectAsState()
    val limitToTop8 by favoritesManager.limitToTop8InDeck.collectAsState()
    val isCurrentStationFavorite = currentStation?.let { favoriteUuids.contains(it.stationuuid) } ?: false

    // Screen On (Display anlassen) State
    val prefs = remember { context.getSharedPreferences("app_deck_prefs", Context.MODE_PRIVATE) }
    var isKeepScreenOn by remember {
        mutableStateOf(prefs.getBoolean("pref_keep_screen_on", false))
    }

    // Radio Layout Mode (Portrait Klassisches Deck vs Landscape Boombox)
    val layoutPref = prefs.getString("pref_radio_layout_mode", RadioLayoutMode.PORTRAIT.name) ?: RadioLayoutMode.PORTRAIT.name
    var radioLayoutMode by remember {
        mutableStateOf(try { RadioLayoutMode.valueOf(layoutPref) } catch (e: Exception) { RadioLayoutMode.PORTRAIT })
    }
    val isSilverMode = radioLayoutMode == RadioLayoutMode.PORTRAIT_ALUMINUM

    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val isLandscapeDevice = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shouldShowLandscape = radioLayoutMode == RadioLayoutMode.LANDSCAPE || radioLayoutMode == RadioLayoutMode.TRANSISTOR || radioLayoutMode == RadioLayoutMode.TRANSISTOR_CREAM || isLandscapeDevice

    // Keep screen orientation aligned with choice when layout mode is explicitly set
    LaunchedEffect(radioLayoutMode) {
        if (radioLayoutMode == RadioLayoutMode.LANDSCAPE || radioLayoutMode == RadioLayoutMode.TRANSISTOR || radioLayoutMode == RadioLayoutMode.TRANSISTOR_CREAM) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Apply or remove FLAG_KEEP_SCREEN_ON on current Activity Window
    DisposableEffect(isKeepScreenOn) {
        if (isKeepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Vollbildmodus (Immersive Mode: Statusleiste & Navigationsleiste ausblenden)
    var isFullscreenEnabled by remember {
        mutableStateOf(prefs.getBoolean("pref_fullscreen_enabled", false))
    }

    DisposableEffect(isFullscreenEnabled, activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreenEnabled) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val frequencyTuningManager = remember { FrequencyTuningManager.getInstance(context) }
    val fmDialMode by frequencyTuningManager.fmDialMode.collectAsState()
    val locationMethod by frequencyTuningManager.locationMethod.collectAsState()
    val activeBnetzaRegion by frequencyTuningManager.activeRegion.collectAsState()
    val isFmMutingEnabled by frequencyTuningManager.isFmMutingEnabled.collectAsState()

    // Pool of selectable stations when radio is active:
    // Entweder echte BNetzA UKW-Sender vor Ort (GPS oder manuelle Stadt) oder gespeicherte Favoriten
    val activeStationPool = remember(favoriteStations, limitToTop8, stations, fmDialMode, locationMethod, activeBnetzaRegion) {
        if (fmDialMode == FmDialMode.LOCATION_BNETZA) {
            frequencyTuningManager.getCurrentBnetzaStations()
        } else {
            val favs = if (limitToTop8) favoriteStations.take(8) else favoriteStations
            if (favs.isNotEmpty()) favs else stations
        }
    }

    // Dial frequency state
    var tunedFrequency by remember { mutableFloatStateOf(currentStation?.dialFrequencyMhz ?: 101.5f) }

    // Synchronize needle when currentStation changes
    LaunchedEffect(currentStation) {
        currentStation?.dialFrequencyMhz?.let { freq ->
            tunedFrequency = freq
        }
    }

    // Bei Auswahl der Transistor-Radios: Radio-Wiedergabe und Tuning initialisieren
    LaunchedEffect(radioLayoutMode) {
        if (radioLayoutMode == RadioLayoutMode.TRANSISTOR || radioLayoutMode == RadioLayoutMode.TRANSISTOR_CREAM) {
            if (playbackSource != PlaybackSource.RADIO) {
                audioEngine.setPlaybackSource(PlaybackSource.RADIO)
            }
            val initialStation = currentStation ?: activeStationPool.firstOrNull() ?: AudioEngine.DEFAULT_STATION
            if (!isRadioPlaying) {
                audioEngine.playStation(initialStation)
            }
            audioEngine.updateTuningFrequency(tunedFrequency, activeStationPool, isFmMutingEnabled)
        }
    }

    // Beim Wechsel von Modus (Favoriten vs. BNetzA), Standortmethode (GPS vs. Manuell) oder Region
    // sicherstellen, dass sofort ein passender Sender aus dem neuen Pool eingestellt und die Skala synchronisiert wird!
    LaunchedEffect(fmDialMode, locationMethod, activeBnetzaRegion) {
        val pool = if (fmDialMode == FmDialMode.LOCATION_BNETZA) {
            frequencyTuningManager.getCurrentBnetzaStations()
        } else {
            val favs = if (limitToTop8) favoriteStations.take(8) else favoriteStations
            if (favs.isNotEmpty()) favs else stations
        }

        if (pool.isNotEmpty() && pool.none { it.stationuuid == currentStation?.stationuuid }) {
            val first = pool.first()
            tunedFrequency = first.dialFrequencyMhz
            if (playbackSource == PlaybackSource.RADIO) {
                audioEngine.playStation(first)
            } else {
                audioEngine.setStation(first)
            }
        }
    }

    // Persist recording automatically whenever a recording session cleanly finishes and finalizes
    LaunchedEffect(Unit) {
        audioEngine.recordingCompletedFlow.collect { result ->
            onSaveCassette(result.cassette, result.trackMarkers)
        }
    }

    val recordingError by audioEngine.recordingError.collectAsState()
    LaunchedEffect(recordingError) {
        recordingError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    var showRecordingOptions by remember { mutableStateOf(false) }
    var recordingMode by remember { mutableStateOf(RecordingMode.CONTINUOUS) }
    val isCueReviewActive by audioEngine.isCueReviewActive.collectAsState()
    val isCueReviewAudioEnabled by audioEngine.cueReviewAudioEnabled.collectAsState()

    // Cassette ONLY spins during recording or during tape playback, or rewind/fast-forward!
    // When playing radio and NOT recording, the tape mechanism does not move.
    val isRewinding = deckState == DeckState.REWINDING
    val isFastForwarding = deckState == DeckState.FAST_FORWARDING
    val isSpinning = (deckState == DeckState.RECORDING || deckState == DeckState.RECORDING_STARTING || deckState == DeckState.RECORDING_STOPPING) ||
            (deckState == DeckState.PLAYING && playbackSource == PlaybackSource.TAPE) ||
            isRewinding || isFastForwarding

    val isRecording = deckState == DeckState.RECORDING || deckState == DeckState.RECORD_ARMED ||
            deckState == DeckState.RECORDING_PAUSED || deckState == DeckState.RECORDING_STARTING || deckState == DeckState.RECORDING_STOPPING

    // Eject tray animation
    val isEjected = deckState == DeckState.EJECTED
    val ejectOffsetY by animateDpAsState(
        targetValue = if (isEjected) (-14).dp else 0.dp,
        animationSpec = tween(durationMillis = 450),
        label = "ejectY"
    )
    val ejectScale by animateFloatAsState(
        targetValue = if (isEjected) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 450),
        label = "ejectScale"
    )

    // Swipe up to open shelf gesture
    val draggableState = rememberDraggableState { delta ->
        if (delta < -30f) {
            onOpenShelf()
        }
    }

    if (radioLayoutMode == RadioLayoutMode.TRANSISTOR_CREAM) {
        CreamTransistorRadioView(
            currentStation = currentStation,
            tunedFrequency = tunedFrequency,
            isCurrentStationFavorite = isCurrentStationFavorite,
            isRadioPlaying = isRadioPlaying,
            onFrequencyChange = { newFreq ->
                tunedFrequency = newFreq
                if (playbackSource != PlaybackSource.RADIO) {
                    audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                }
                audioEngine.updateTuningFrequency(newFreq, activeStationPool, isFmMutingEnabled)
                val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                closest?.let {
                    val dist = kotlin.math.abs(it.dialFrequencyMhz - newFreq)
                    if (dist <= 0.15f) {
                        if (it.stationuuid != currentStation?.stationuuid || !isRadioPlaying) {
                            audioEngine.playStation(it)
                        }
                    }
                }
            },
            onPowerSwitch = { isPowered ->
                if (isPowered) {
                    audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                    val st = currentStation ?: activeStationPool.firstOrNull() ?: AudioEngine.DEFAULT_STATION
                    audioEngine.playStation(st)
                    audioEngine.updateTuningFrequency(tunedFrequency, activeStationPool, isFmMutingEnabled)
                } else {
                    audioEngine.setPlaybackSource(PlaybackSource.TAPE)
                }
            },
            onToggleFavorite = {
                currentStation?.let { st -> favoritesManager.toggleFavorite(st) }
            },
            onOpenStations = onOpenStations,
            onOpenSettings = { showRecordingOptions = true }
        )
    } else if (radioLayoutMode == RadioLayoutMode.TRANSISTOR) {
        TransistorRadioView(
            currentStation = currentStation,
            tunedFrequency = tunedFrequency,
            isCurrentStationFavorite = isCurrentStationFavorite,
            isRadioPlaying = isRadioPlaying,
            onFrequencyChange = { newFreq ->
                tunedFrequency = newFreq
                if (playbackSource != PlaybackSource.RADIO) {
                    audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                }
                audioEngine.updateTuningFrequency(newFreq, activeStationPool, isFmMutingEnabled)
                val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                closest?.let {
                    val dist = kotlin.math.abs(it.dialFrequencyMhz - newFreq)
                    if (dist <= 0.15f) {
                        if (it.stationuuid != currentStation?.stationuuid || !isRadioPlaying) {
                            audioEngine.playStation(it)
                        }
                    }
                }
            },
            onPowerModeChange = { mode ->
                when (mode) {
                    de.tedoki.mixtape.ui.screens.TransistorBandPowerMode.OFF -> {
                        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
                    }
                    de.tedoki.mixtape.ui.screens.TransistorBandPowerMode.FM -> {
                        audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                        val st = currentStation ?: activeStationPool.firstOrNull() ?: AudioEngine.DEFAULT_STATION
                        audioEngine.playStation(st)
                        audioEngine.updateTuningFrequency(tunedFrequency, activeStationPool, isFmMutingEnabled)
                    }
                    de.tedoki.mixtape.ui.screens.TransistorBandPowerMode.AM -> {
                        audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                        // In AM mode without FM stations, tune to empty pool for static
                        audioEngine.updateTuningFrequency(tunedFrequency, emptyList(), false)
                    }
                }
            },
            onToggleFavorite = {
                currentStation?.let { st -> favoritesManager.toggleFavorite(st) }
            },
            onOpenStations = onOpenStations,
            onOpenSettings = { showRecordingOptions = true }
        )
    } else if (shouldShowLandscape) {
        LandscapeBoomboxView(
            deckState = deckState,
            playbackSource = playbackSource,
            currentStation = currentStation,
            activeTape = activeTape,
            tapePositionSec = tapePositionSec,
            isRadioPlaying = isRadioPlaying,
            nowPlaying = nowPlaying,
            vuLeft = vuLeft,
            vuRight = vuRight,
            counter = counter,
            tunedFrequency = tunedFrequency,
            isCurrentStationFavorite = isCurrentStationFavorite,
            isSpinning = isSpinning,
            isRewinding = isRewinding,
            isFastForwarding = isFastForwarding,
            isRecording = isRecording,
            isEjected = isEjected,
            isKeepScreenOn = isKeepScreenOn,
            onToggleKeepScreenOn = {
                val newState = !isKeepScreenOn
                isKeepScreenOn = newState
                prefs.edit().putBoolean("pref_keep_screen_on", newState).apply()
                val msg = if (newState) "Display bleibt an" else "Automatisches Display-Timeout aktiv"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onOpenStations = onOpenStations,
            onOpenSettings = { showRecordingOptions = true },
            onOpenShelf = onOpenShelf,
            onOpenCustomizer = onOpenCustomizer,
            onOpenDetail = onOpenDetail,
            onFrequencyChange = { newFreq ->
                tunedFrequency = newFreq
                audioEngine.updateTuningFrequency(newFreq, activeStationPool, isFmMutingEnabled)
                val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                closest?.let {
                    // Schalte den Sender ein/um, sobald die Nadel nah genug am Sender ist (< 0.15 MHz)
                    if (kotlin.math.abs(it.dialFrequencyMhz - newFreq) <= 0.15f && it.stationuuid != currentStation?.stationuuid) {
                        if (playbackSource == PlaybackSource.RADIO) {
                            audioEngine.playStation(it)
                        } else {
                            audioEngine.setStation(it)
                        }
                    }
                }
            },
            onToggleRadioPlayback = { audioEngine.toggleRadioPlayback() },
            onToggleFavorite = {
                currentStation?.let { st -> favoritesManager.toggleFavorite(st) }
            },
            onSourceChanged = { audioEngine.setPlaybackSource(it) },
            onResetCounter = { audioEngine.resetCounter() },
            onRecordPress = { audioEngine.onRecordPress() },
            onPlayPress = { audioEngine.onPlayPress() },
            onRewindPress = { audioEngine.onRewindPress() },
            onFastForwardPress = { audioEngine.onFastForwardPress() },
            onPausePress = { audioEngine.onPausePress() },
            onStopEjectPress = { audioEngine.onStopEjectPress() },
            onInsertTape = {
                val tape = activeTape ?: Cassette(
                    id = "tape_default",
                    title = "Mein Mixtape",
                    notes = "1LIVE",
                    colorHex = "#212328"
                )
                audioEngine.insertTape(tape)
            },
            isCueReviewActive = isCueReviewActive
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSilverMode) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E222A),
                                Color(0xFF14171E),
                                Color(0xFF0D0F13)
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(DeckSurfaceDark, DeckSurfaceDark))
                    }
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hi-Fi Deck Top Beveled Header with Extruded Metallic Nameplate Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extruded brushed aluminum badge plate
                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = if (isSilverMode) {
                                    listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFE4E9F2),
                                        Color(0xFFCAD2E0)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF323844),
                                        Color(0xFF20232A),
                                        Color(0xFF14161B)
                                    )
                                }
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = if (isSilverMode) {
                                    listOf(Color(0xFFFFFFFF), Color(0xFF8C97A8))
                                } else {
                                    listOf(Color(0xFF6B768A), Color(0xFF242730))
                                }
                            ),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tedoki",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSilverMode) Color(0xFF1A1D24) else Color(0xFFF1F5F9),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        // Recessed sub-badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSilverMode) Color(0xFF8B1D23) else Color(0xFF241505))
                                .border(
                                    0.5.dp,
                                    if (isSilverMode) Color(0xFFC62828) else Color(0xFF664112),
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "MIXTAPE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSilverMode) Color(0xFFFFFFFF) else Color(0xFFFFB300),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                // Top Tactile Action Buttons with metallic collar rings
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Display Always On / Keep Screen On Toggle (links neben Radio-Button)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isKeepScreenOn) {
                                        listOf(Color(0xFF5A440D), Color(0xFF261D05))
                                    } else if (isSilverMode) {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFDEE4EE), Color(0xFFB5BFD0))
                                    } else {
                                        listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                    }
                                )
                            )
                            .border(
                                1.dp,
                                if (isKeepScreenOn) Color(0xFFFFB300) else if (isSilverMode) Color(0xFF7E8A9C) else Color(0xFF454C5A),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val newState = !isKeepScreenOn
                                isKeepScreenOn = newState
                                prefs.edit().putBoolean("pref_keep_screen_on", newState).apply()
                                val msg = if (newState) "Display bleibt an" else "Automatisches Display-Timeout aktiv"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessHigh,
                                contentDescription = if (isKeepScreenOn) "Display dauerhaft an (aktiviert)" else "Display dauerhaft an (deaktiviert)",
                                tint = if (isKeepScreenOn) Color(0xFFFFC107) else if (isSilverMode) Color(0xFF5A6474) else Color(0xFF8B95A5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Open Radio Stations
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSilverMode) {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFDEE4EE), Color(0xFFB5BFD0))
                                    } else {
                                        listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                    }
                                )
                            )
                            .border(1.dp, if (isSilverMode) Color(0xFF7E8A9C) else Color(0xFF454C5A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenStations,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radiosender",
                                tint = if (isSilverMode) Color(0xFF263238) else Color(0xFFCFD8DC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Open Recording Options
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSilverMode) {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFDEE4EE), Color(0xFFB5BFD0))
                                    } else {
                                        listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                    }
                                )
                            )
                            .border(1.dp, if (isSilverMode) Color(0xFF7E8A9C) else Color(0xFF454C5A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showRecordingOptions = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Aufnahmeoptionen",
                                tint = if (isSilverMode) Color(0xFF263238) else Color(0xFFCFD8DC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Open Cassette Shelf
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSilverMode) {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFDEE4EE), Color(0xFFBAC4D4))
                                    } else {
                                        listOf(Color(0xFF382A12), Color(0xFF1E170A))
                                    }
                                )
                            )
                            .border(1.dp, if (isSilverMode) Color(0xFF7E8A9C) else Color(0xFF7A581F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenShelf,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Kassettensammlung",
                                tint = if (isSilverMode) Color(0xFFB45309) else Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Open Audio File Information for current tape
                    if (activeTape != null && onOpenDetail != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSilverMode) {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFDEE4EE), Color(0xFFB5BFD0))
                                    } else {
                                        listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                    }
                                )
                            )
                            .border(1.dp, if (isSilverMode) Color(0xFF7E8A9C) else Color(0xFF454C5A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { onOpenDetail(activeTape!!) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Audiodatei-Informationen",
                                    tint = if (isSilverMode) Color(0xFF263238) else Color(0xFFCFD8DC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Chassis Housing Frame (Dark Anthracite or Brushed Silver Aluminum)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(if (isSilverMode) 20.dp else 16.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSilverMode) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF6F8FB),
                                    Color(0xFFE6EAF2),
                                    Color(0xFFD6DCE6),
                                    Color(0xFFCAD0DC),
                                    Color(0xFFBCC3CF),
                                    Color(0xFFE8ECF5)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2F343E),
                                    DeckAnthracite,
                                    Color(0xFF101215)
                                )
                            )
                        }
                    )
                    .border(
                        2.dp,
                        if (isSilverMode) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFCBD3DF),
                                    Color(0xFF8F9BAE)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF6E788B),
                                    Color(0xFF333842),
                                    Color(0xFF1B1D22)
                                )
                            )
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Top Chassis Screw Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HiFiCornerScrew(isSilverStyle = isSilverMode)
                        HiFiCornerScrew(isSilverStyle = isSilverMode)
                    }

                    if (isSilverMode) {
                        // Perforierte Aluminium-Lautsprecherblende (Hochformat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFE4E9F2),
                                            Color(0xFFD3DBE7),
                                            Color(0xFFC4CBD7),
                                            Color(0xFFBCC4D2)
                                        )
                                    )
                                )
                                .border(
                                    0.8.dp,
                                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFF8B95A5))),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "STEREO RECORDER",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF374151),
                                letterSpacing = 1.sp
                            )
                            // Perforierte Aluminium-Lautsprecherlöcher
                            Canvas(
                                modifier = Modifier
                                    .width(130.dp)
                                    .fillMaxHeight()
                            ) {
                                val holeR = 1.6.dp.toPx()
                                val step = 4.2.dp.toPx()
                                val cols = (size.width / step).toInt()
                                val rows = (size.height / step).toInt().coerceAtLeast(1)
                                for (r in 0 until rows) {
                                    val y = 2.dp.toPx() + r * step
                                    for (c in 0 until cols) {
                                        val x = 2.dp.toPx() + c * step + if (r % 2 == 1) step * 0.5f else 0f
                                        if (x < size.width - 2.dp.toPx()) {
                                            drawCircle(color = Color(0xFFFFFFFF), radius = holeR + 0.5.dp.toPx(), center = Offset(x, y + 0.5.dp.toPx()))
                                            drawCircle(color = Color(0xFF1E2229), radius = holeR, center = Offset(x, y))
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "SOLID ALUMINUM",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF374151),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // 1. Authentic Cassette Bay Door with Acrylic Viewing Window (Kassettendeckel mit Sichtfenster)
                    val currentTape = activeTape ?: Cassette(
                        id = "tape_default",
                        title = if (isRecording) "Radioaufnahme" else "Mein Mixtape",
                        notes = "Radioaufnahme 1LIVE",
                        colorHex = "#212328"
                    )

                    CassetteDoorCompartment(
                        cassette = currentTape,
                        isSpinning = isSpinning,
                        isRewinding = isRewinding,
                        isFastForwarding = isFastForwarding,
                        isRecording = isRecording,
                        isEjected = isEjected,
                        isSilverStyle = isSilverMode,
                        tapePositionSec = tapePositionSec,
                        totalCapacitySec = currentTape.totalCapacitySec,
                        onLabelClick = {
                            onOpenCustomizer(currentTape)
                        },
                        onOpenCustomizer = onOpenCustomizer,
                        onOpenDetail = onOpenDetail,
                        onInsertTape = {
                            audioEngine.insertTape(currentTape)
                        }
                    )

                    HiFiSectionDivider(isSilverStyle = isSilverMode)

                    val isTapeMode = playbackSource == PlaybackSource.TAPE
                    val isTapeRunning = isSpinning && isTapeMode

                    // 2. Analog FM Tuning Scale: Swiping switches between favorite stations
                    TuningScale(
                        currentFrequencyMhz = tunedFrequency,
                        onFrequencyChange = { newFreq ->
                            tunedFrequency = newFreq
                            audioEngine.updateTuningFrequency(newFreq, activeStationPool, isFmMutingEnabled)
                            val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (kotlin.math.abs(it.dialFrequencyMhz - newFreq) <= 0.15f && it.stationuuid != currentStation?.stationuuid) {
                                    if (playbackSource == PlaybackSource.RADIO) {
                                        audioEngine.playStation(it)
                                    } else {
                                        audioEngine.setStation(it)
                                    }
                                }
                            }
                        },
                        isDimmed = isTapeMode,
                        isSilverStyle = isSilverMode
                    )

                    // 2b. Tactile FM Frequency Slider (unter der Skala, mit metallischem Knopf & rotem Strich)
                    FrequencySlider(
                        currentFrequencyMhz = tunedFrequency,
                        onFrequencyChange = { newFreq ->
                            tunedFrequency = newFreq
                            audioEngine.updateTuningFrequency(newFreq, activeStationPool, isFmMutingEnabled)
                            val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (kotlin.math.abs(it.dialFrequencyMhz - newFreq) <= 0.15f && it.stationuuid != currentStation?.stationuuid) {
                                    if (playbackSource == PlaybackSource.RADIO) {
                                        audioEngine.playStation(it)
                                    } else {
                                        audioEngine.setStation(it)
                                    }
                                }
                            }
                        },
                        isDimmed = isTapeMode,
                        isSilverStyle = isSilverMode,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    HiFiSectionDivider(isSilverStyle = isSilverMode)

                    // 3. Digital Radio Panel (with Station in 14-Segment/Matrix, Now Playing, LEDs & Favorite Star)
                    DigitalRadioPanel(
                        station = currentStation,
                        nowPlayingText = nowPlaying,
                        isRecording = isRecording,
                        isFavorite = isCurrentStationFavorite,
                        isDimmed = isTapeMode,
                        isTapeRunning = isTapeRunning,
                        isRadioPlaying = isRadioPlaying,
                        isSilverStyle = isSilverMode,
                        onToggleRadioPlayback = {
                            audioEngine.toggleRadioPlayback()
                        },
                        onPanelClick = {
                            if (isTapeMode) {
                                audioEngine.setPlaybackSource(PlaybackSource.RADIO)
                            } else {
                                onOpenStations()
                            }
                        },
                        onToggleFavorite = {
                            currentStation?.let { st ->
                                favoritesManager.toggleFavorite(st)
                            }
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    HiFiSectionDivider(isSilverStyle = isSilverMode)

                    // 4. Source Switch (links neben dem Zähler), Tape Counter & Stereo VU-Meter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Schalter links neben dem Zähler (kleiner & kompakt)
                        FunctionSelectorSwitch(
                            currentSource = playbackSource,
                            isSilverStyle = isSilverMode,
                            onSourceChanged = { newSource ->
                                audioEngine.setPlaybackSource(newSource)
                            }
                        )

                        // Mechanischer Zähler für die Kassette (000 = Anfang der Kassette) mit Reset-Knopf
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "COUNTER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSilverMode) Color(0xFF263238) else Color(0xFF90A4AE),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            MechanicalTapeCounter(
                                value = counter,
                                isSilverStyle = isSilverMode,
                                onReset = { audioEngine.resetCounter() }
                            )
                        }

                        // Stereo Dual VU-Meter
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VU LEVEL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSilverMode) Color(0xFF263238) else Color(0xFF90A4AE),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            DualVuMeter(
                                levelLeft = vuLeft,
                                levelRight = vuRight,
                                isSilverStyle = isSilverMode,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HiFiSectionDivider(isSilverStyle = isSilverMode)

                    // 5. Tactile Mechanical Transport Controls
                    TransportControls(
                        deckState = deckState,
                        isSilverStyle = isSilverMode,
                        isCueReviewActive = isCueReviewActive,
                        onRecordClick = {
                            audioEngine.onRecordPress()
                        },
                        onPlayClick = {
                            audioEngine.onPlayPress()
                        },
                        onRewindClick = {
                            audioEngine.onRewindPress()
                        },
                        onFastForwardClick = {
                            audioEngine.onFastForwardPress()
                        },
                        onPauseClick = {
                            audioEngine.onPausePress()
                        },
                        onEjectClick = {
                            audioEngine.onStopEjectPress()
                        }
                    )

                    // Bottom Chassis Screw Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HiFiCornerScrew(isSilverStyle = isSilverMode)
                        Text(
                            text = "HIGH FIDELITY COMPACT CASSETTE",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSilverMode) Color(0xFF4A5568) else Color(0xFF555D6B),
                            letterSpacing = 1.sp
                        )
                        HiFiCornerScrew(isSilverStyle = isSilverMode)
                    }
                }
            }

            // Bottom drawer hint: "Kassettensammlung öffnen"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenShelf() }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "▲ Nach oben wischen für Kassettensammlung",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )
            }
        }
    }
    }

    // Settings Bottom Sheet (Submenüs: "Radio auswählen", "Frequenzenstellung" & "MP3 Aufnahme")
    if (showRecordingOptions) {
        SettingsSheet(
            currentRecordingMode = recordingMode,
            currentLayoutMode = radioLayoutMode,
            frequencyTuningManager = frequencyTuningManager,
            currentTunedStation = currentStation,
            onTuneStation = { station ->
                tunedFrequency = station.dialFrequencyMhz
                if (playbackSource == PlaybackSource.RADIO) {
                    audioEngine.playStation(station)
                } else {
                    audioEngine.setStation(station)
                }
            },
            onRecordingModeSelect = {
                recordingMode = it
            },
            onRadioLayoutModeSelect = { newLayout ->
                radioLayoutMode = newLayout
                prefs.edit().putString("pref_radio_layout_mode", newLayout.name).apply()
                if (newLayout == RadioLayoutMode.LANDSCAPE || newLayout == RadioLayoutMode.TRANSISTOR || newLayout == RadioLayoutMode.TRANSISTOR_CREAM) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            },
            isCueReviewAudioEnabled = isCueReviewAudioEnabled,
            onCueReviewAudioEnabledChange = { audioEngine.setCueReviewAudioEnabled(it) },
            isFullscreenEnabled = isFullscreenEnabled,
            onFullscreenEnabledChange = { enabled ->
                isFullscreenEnabled = enabled
                prefs.edit().putBoolean("pref_fullscreen_enabled", enabled).apply()
            },
            onDismiss = { showRecordingOptions = false }
        )
    }
}

@Composable
fun HiFiCornerScrew(
    modifier: Modifier = Modifier,
    isSilverStyle: Boolean = false
) {
    Box(
        modifier = modifier
            .size(11.dp)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isSilverStyle) {
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFC4CBD7),
                            Color(0xFF7A8699)
                        )
                    } else {
                        listOf(
                            Color(0xFF9CA3AF),
                            Color(0xFF4B5563),
                            Color(0xFF1F242C)
                        )
                    }
                )
            )
            .border(
                0.5.dp,
                if (isSilverStyle) Color(0xFF6B7280) else Color(0xFF111317),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Machined screw slot
        Box(
            modifier = Modifier
                .width(6.5.dp)
                .height(1.4.dp)
                .background(if (isSilverStyle) Color(0xFF2E3440) else Color(0xFF0B0C0E), RoundedCornerShape(0.5.dp))
        )
    }
}

@Composable
fun HiFiSectionDivider(
    modifier: Modifier = Modifier,
    isSilverStyle: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        // Upper recessed groove
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isSilverStyle) Color(0xFF9AA5B8) else Color(0xFF08090C))
        )
        // Lower metallic bevel highlight line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = if (isSilverStyle) {
                            listOf(
                                Color(0x10FFFFFF),
                                Color(0xDDFFFFFF),
                                Color(0x10FFFFFF)
                            )
                        } else {
                            listOf(
                                Color(0x054B5563),
                                Color(0x604B5563),
                                Color(0x054B5563)
                            )
                        }
                    )
                )
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
