package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.data.FavoritesManager
import com.example.model.Cassette
import com.example.model.DeckState
import com.example.model.PlaybackSource
import com.example.model.RadioStation
import com.example.model.RecordingMode
import com.example.model.TrackMarker
import com.example.ui.components.CassetteDoorCompartment
import com.example.ui.components.CassetteView
import com.example.ui.components.DigitalRadioPanel
import com.example.ui.components.DualVuMeter
import com.example.ui.components.FrequencySlider
import com.example.ui.components.FunctionSelectorSwitch
import com.example.ui.components.MechanicalTapeCounter
import com.example.ui.components.RecordingOptionsSheet
import com.example.ui.components.TransportControls
import com.example.ui.components.TuningScale
import com.example.ui.components.VuMeterAndCounter
import com.example.ui.theme.DeckAnthracite
import com.example.ui.theme.DeckChassisBevel
import com.example.ui.theme.DeckChassisHighlight
import com.example.ui.theme.DeckSurfaceDark

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

    // Apply or remove FLAG_KEEP_SCREEN_ON on current Activity Window
    DisposableEffect(isKeepScreenOn) {
        val activity = context.findActivity()
        if (isKeepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Pool of selectable stations when radio is active: strictly favorites (limited to top 8 if setting enabled), fallback to curated
    val activeStationPool = remember(favoriteStations, limitToTop8, stations) {
        val favs = if (limitToTop8) favoriteStations.take(8) else favoriteStations
        if (favs.isNotEmpty()) favs else stations
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

    // Dial frequency state
    var tunedFrequency by remember { mutableFloatStateOf(currentStation?.dialFrequencyMhz ?: 101.5f) }

    // Synchronize needle when currentStation changes
    LaunchedEffect(currentStation) {
        currentStation?.dialFrequencyMhz?.let { freq ->
            tunedFrequency = freq
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
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
                                colors = listOf(
                                    Color(0xFF323844),
                                    Color(0xFF20232A),
                                    Color(0xFF14161B)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF6B768A), Color(0xFF242730))
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
                            color = Color(0xFFF1F5F9),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        // Recessed amber sub-badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF241505))
                                .border(0.5.dp, Color(0xFF664112), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "MIXTAPE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB300),
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
                                    } else {
                                        listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                    }
                                )
                            )
                            .border(
                                1.dp,
                                if (isKeepScreenOn) Color(0xFFFFB300) else Color(0xFF454C5A),
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
                                tint = if (isKeepScreenOn) Color(0xFFFFC107) else Color(0xFF8B95A5),
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
                                    colors = listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                )
                            )
                            .border(1.dp, Color(0xFF454C5A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenStations,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radiosender",
                                tint = Color(0xFFCFD8DC),
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
                                    colors = listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                )
                            )
                            .border(1.dp, Color(0xFF454C5A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showRecordingOptions = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Aufnahmeoptionen",
                                tint = Color(0xFFCFD8DC),
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
                                    colors = listOf(Color(0xFF382A12), Color(0xFF1E170A))
                                )
                            )
                            .border(1.dp, Color(0xFF7A581F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenShelf,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Kassettensammlung",
                                tint = Color(0xFFFFB300),
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
                                    colors = listOf(Color(0xFF2E333C), Color(0xFF191B20))
                                )
                            )
                            .border(1.dp, Color(0xFF454C5A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { onOpenDetail(activeTape!!) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Audiodatei-Informationen",
                                    tint = Color(0xFFCFD8DC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Chassis Housing Frame (Dark Anthracite with high-depth brushed metal texture and bevels)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2F343E),
                                DeckAnthracite,
                                Color(0xFF101215)
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6E788B),
                                Color(0xFF333842),
                                Color(0xFF1B1D22)
                            )
                        ),
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
                        HiFiCornerScrew()
                        HiFiCornerScrew()
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

                    HiFiSectionDivider()

                    val isTapeMode = playbackSource == PlaybackSource.TAPE
                    val isTapeRunning = isSpinning && isTapeMode

                    // 2. Analog FM Tuning Scale: Swiping switches between favorite stations
                    TuningScale(
                        currentFrequencyMhz = tunedFrequency,
                        onFrequencyChange = { newFreq ->
                            tunedFrequency = newFreq
                            // Find closest station from active favorites pool
                            val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (it.stationuuid != currentStation?.stationuuid) {
                                    if (playbackSource == PlaybackSource.RADIO) {
                                        audioEngine.playStation(it)
                                    } else {
                                        audioEngine.setStation(it)
                                    }
                                }
                            }
                        },
                        isDimmed = isTapeMode
                    )

                    // 2b. Tactile FM Frequency Slider (unter der Skala, mit metallischem Knopf & rotem Strich)
                    FrequencySlider(
                        currentFrequencyMhz = tunedFrequency,
                        onFrequencyChange = { newFreq ->
                            tunedFrequency = newFreq
                            // Gleiche bewährte Abstimm-Logik wie bei der Skala:
                            val closest = activeStationPool.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (it.stationuuid != currentStation?.stationuuid) {
                                    if (playbackSource == PlaybackSource.RADIO) {
                                        audioEngine.playStation(it)
                                    } else {
                                        audioEngine.setStation(it)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    HiFiSectionDivider()

                    // 3. Digital Radio Panel (with Station in 14-Segment/Matrix, Now Playing, LEDs & Favorite Star)
                    DigitalRadioPanel(
                        station = currentStation,
                        nowPlayingText = nowPlaying,
                        isRecording = isRecording,
                        isFavorite = isCurrentStationFavorite,
                        isDimmed = isTapeMode,
                        isTapeRunning = isTapeRunning,
                        isRadioPlaying = isRadioPlaying,
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

                    HiFiSectionDivider()

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
                                color = Color(0xFF90A4AE),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            MechanicalTapeCounter(
                                value = counter,
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
                                color = Color(0xFF90A4AE),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            DualVuMeter(
                                levelLeft = vuLeft,
                                levelRight = vuRight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HiFiSectionDivider()

                    // 5. Tactile Mechanical Transport Controls
                    TransportControls(
                        deckState = deckState,
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
                        HiFiCornerScrew()
                        Text(
                            text = "HIGH FIDELITY COMPACT CASSETTE",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555D6B),
                            letterSpacing = 1.sp
                        )
                        HiFiCornerScrew()
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

    // Recording Options Bottom Sheet
    if (showRecordingOptions) {
        RecordingOptionsSheet(
            currentMode = recordingMode,
            onModeSelect = {
                recordingMode = it
                showRecordingOptions = false
            },
            onDismiss = { showRecordingOptions = false }
        )
    }
}

@Composable
fun HiFiCornerScrew(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(11.dp)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9CA3AF),
                        Color(0xFF4B5563),
                        Color(0xFF1F242C)
                    )
                )
            )
            .border(0.5.dp, Color(0xFF111317), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Machined screw slot
        Box(
            modifier = Modifier
                .width(6.5.dp)
                .height(1.4.dp)
                .background(Color(0xFF0B0C0E), RoundedCornerShape(0.5.dp))
        )
    }
}

@Composable
fun HiFiSectionDivider(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        // Upper recessed shadow line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF08090C))
        )
        // Lower metallic bevel highlight line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x054B5563),
                            Color(0x604B5563),
                            Color(0x054B5563)
                        )
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
