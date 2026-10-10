package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.CassetteView
import com.example.ui.components.DigitalRadioPanel
import com.example.ui.components.DualVuMeter
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
    onSaveCassette: (Cassette, List<TrackMarker>) -> Unit
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
    val favoriteUuids by favoritesManager.favoriteUuids.collectAsState()
    val isCurrentStationFavorite = currentStation?.let { favoriteUuids.contains(it.stationuuid) } ?: false

    var showRecordingOptions by remember { mutableStateOf(false) }
    var recordingMode by remember { mutableStateOf(RecordingMode.CONTINUOUS) }

    // Dial frequency state
    var tunedFrequency by remember { mutableFloatStateOf(currentStation?.dialFrequencyMhz ?: 101.5f) }

    // Cassette ONLY spins during recording or during tape playback, or rewind/fast-forward!
    // When playing radio and NOT recording, the tape mechanism does not move.
    val isRewinding = deckState == DeckState.REWINDING
    val isFastForwarding = deckState == DeckState.FAST_FORWARDING
    val isSpinning = (deckState == DeckState.RECORDING) ||
            (deckState == DeckState.PLAYING && playbackSource == PlaybackSource.TAPE) ||
            isRewinding || isFastForwarding

    val isRecording = deckState == DeckState.RECORDING || deckState == DeckState.RECORD_ARMED || deckState == DeckState.RECORDING_PAUSED

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
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hi-Fi Deck Top Beveled Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tedoki",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MIXTAPE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        letterSpacing = 2.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open Radio Stations
                    IconButton(
                        onClick = onOpenStations,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radiosender",
                            tint = Color(0xFFCFD8DC)
                        )
                    }

                    // Open Recording Options
                    IconButton(
                        onClick = { showRecordingOptions = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Aufnahmeoptionen",
                            tint = Color(0xFFCFD8DC)
                        )
                    }

                    // Open Cassette Shelf
                    IconButton(
                        onClick = onOpenShelf,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Kassettensammlung",
                            tint = Color(0xFFFFB300)
                        )
                    }
                }
            }

            // Chassis Housing Frame (Dark Anthracite with brushed metal texture)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeckChassisBevel,
                                DeckAnthracite,
                                Color(0xFF131518)
                            )
                        )
                    )
                    .border(2.dp, DeckChassisHighlight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Cassette Bay Window with inserted Cassette
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1114))
                            .border(2.dp, Color(0xFF2C3038), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentTape = activeTape ?: Cassette(
                            id = "tape_default",
                            title = if (isRecording) "Radioaufnahme" else "Mein Mixtape",
                            notes = "Radioaufnahme 1LIVE",
                            colorHex = "#212328"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = ejectOffsetY)
                        ) {
                            CassetteView(
                                cassette = currentTape,
                                isSpinning = isSpinning,
                                isRewinding = isRewinding,
                                isFastForwarding = isFastForwarding,
                                tapePositionSec = tapePositionSec,
                                totalCapacitySec = currentTape.totalCapacitySec,
                                modifier = Modifier.fillMaxWidth(),
                                onLabelClick = {
                                    onOpenCustomizer(currentTape)
                                }
                            )
                        }

                        // Eject Action Overlay when ejected
                        if (isEjected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Kassette ausgeworfen",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onOpenCustomizer(currentTape) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B55))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Beschriften")
                                        }

                                        Button(
                                            onClick = {
                                                audioEngine.insertTape(currentTape)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                                        ) {
                                            Text("Einlegen", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val isTapeMode = playbackSource == PlaybackSource.TAPE
                    val isTapeRunning = isSpinning && isTapeMode

                    // 2. Analog FM Tuning Scale
                    TuningScale(
                        currentFrequencyMhz = tunedFrequency,
                        onFrequencyChange = { newFreq ->
                            tunedFrequency = newFreq
                            // Find closest radio station
                            val closest = stations.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (playbackSource == PlaybackSource.RADIO) {
                                    audioEngine.playStation(it)
                                } else {
                                    audioEngine.setStation(it)
                                }
                            }
                        },
                        isDimmed = isTapeMode
                    )

                    // 3. Digital Radio Panel (with Station, Now Playing, LEDs & Tuning Knob)
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
                        onPreviousStation = {
                            val currentIndex = stations.indexOfFirst { it.stationuuid == currentStation?.stationuuid }
                            val newIndex = if (currentIndex <= 0) stations.size - 1 else currentIndex - 1
                            val newStation = stations[newIndex]
                            tunedFrequency = newStation.dialFrequencyMhz
                            if (playbackSource == PlaybackSource.RADIO) {
                                audioEngine.playStation(newStation)
                            } else {
                                audioEngine.setStation(newStation)
                            }
                        },
                        onNextStation = {
                            val currentIndex = stations.indexOfFirst { it.stationuuid == currentStation?.stationuuid }
                            val newIndex = if (currentIndex < 0 || currentIndex >= stations.size - 1) 0 else currentIndex + 1
                            val newStation = stations[newIndex]
                            tunedFrequency = newStation.dialFrequencyMhz
                            if (playbackSource == PlaybackSource.RADIO) {
                                audioEngine.playStation(newStation)
                            } else {
                                audioEngine.setStation(newStation)
                            }
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
                                favoritesManager.toggleFavorite(st.stationuuid)
                            }
                        },
                        onRotateKnob = { delta ->
                            val newFreq = (tunedFrequency + delta * 0.05f).coerceIn(88.0f, 108.0f)
                            tunedFrequency = newFreq
                            val closest = stations.minByOrNull { kotlin.math.abs(it.dialFrequencyMhz - newFreq) }
                            closest?.let {
                                if (playbackSource == PlaybackSource.RADIO) {
                                    audioEngine.playStation(it)
                                } else {
                                    audioEngine.setStation(it)
                                }
                            }
                        }
                    )

                    // 4. Source Switch (links neben dem Zähler), Tape Counter & Stereo VU-Meter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Schalter links neben dem Zähler (kleiner & kompakt)
                        FunctionSelectorSwitch(
                            currentSource = playbackSource,
                            onSourceChanged = { newSource ->
                                val recordingResult = audioEngine.setPlaybackSource(newSource)
                                recordingResult?.let { result ->
                                    onSaveCassette(result.cassette, result.trackMarkers)
                                }
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

                    // 5. Tactile Mechanical Transport Controls
                    TransportControls(
                        deckState = deckState,
                        onRecordClick = {
                            audioEngine.onRecordPress()
                        },
                        onPlayClick = {
                            val recordingResult = audioEngine.onPlayPress()
                            recordingResult?.let { result ->
                                onSaveCassette(result.cassette, result.trackMarkers)
                            }
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
                            val recordingResult = audioEngine.onStopEjectPress()
                            recordingResult?.let { result ->
                                onSaveCassette(result.cassette, result.trackMarkers)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom drawer hint: "Kassettensammlung öffnen"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenShelf() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "▲ Nach oben wischen für Kassettensammlung",
                    fontSize = 12.sp,
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
