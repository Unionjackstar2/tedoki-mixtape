package de.tedoki.mixtape.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.DeckState
import de.tedoki.mixtape.model.PlaybackSource
import de.tedoki.mixtape.model.RadioStation
import de.tedoki.mixtape.ui.components.CassetteDoorCompartment
import de.tedoki.mixtape.ui.components.DigitalRadioPanel
import de.tedoki.mixtape.ui.components.DualVuMeter
import de.tedoki.mixtape.ui.components.FrequencySlider
import de.tedoki.mixtape.ui.components.FunctionSelectorSwitch
import de.tedoki.mixtape.ui.components.MechanicalTapeCounter
import de.tedoki.mixtape.ui.components.TransportControls
import de.tedoki.mixtape.ui.components.TuningScale

/**
 * Hi-Fi Radiokassettenrekorder im Querformat.
 * Exakt nach Vorlage:
 * - Echtes, silber gebürstetes Aluminium (Silberner Aluminium-Korpus mit horizontaler Bürstung und CNC-Schliff)
 * - Schmale Lautsprecherleisten links & rechts mit direkt ins Aluminium gestanzten/gebohrten Lautsprecherlöchern
 *   (KEINE runden Lautsprecher oder großen Konusse, sondern schmale perforierte Aluminium-Lochmatrix)
 * - Silber gefasstes Kassettenfach mit Acrylglas-Sichtfenster und echten rotierenden Bandspulen
 * - Hochpräziser Radiotuner mit digitalem LCD-Display, analoger beleuchteter FM-Skala und Frequenz-Slider
 * - Mechanische Klaviatur (6 Transporttasten) in gefrästem Silber-Aluminium
 * - Quellenschalter (TAPE / RADIO), Bandzählwerk und Stereo VU-Meter
 */
@Composable
fun LandscapeBoomboxView(
    deckState: DeckState,
    playbackSource: PlaybackSource,
    currentStation: RadioStation?,
    activeTape: Cassette?,
    tapePositionSec: Int,
    isRadioPlaying: Boolean,
    nowPlaying: String,
    vuLeft: Int,
    vuRight: Int,
    counter: Int,
    tunedFrequency: Float,
    isCurrentStationFavorite: Boolean,
    isSpinning: Boolean,
    isRewinding: Boolean,
    isFastForwarding: Boolean,
    isRecording: Boolean,
    isEjected: Boolean,
    isKeepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onOpenStations: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenCustomizer: (Cassette) -> Unit,
    onOpenDetail: ((Cassette) -> Unit)?,
    onFrequencyChange: (Float) -> Unit,
    onToggleRadioPlayback: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSourceChanged: (PlaybackSource) -> Unit,
    onResetCounter: () -> Unit,
    onRecordPress: () -> Unit,
    onPlayPress: () -> Unit,
    onRewindPress: () -> Unit,
    onFastForwardPress: () -> Unit,
    onPausePress: () -> Unit,
    onStopEjectPress: () -> Unit,
    onInsertTape: () -> Unit,
    isCueReviewActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isTapeMode = playbackSource == PlaybackSource.TAPE
    val isTapeRunning = isSpinning && isTapeMode

    val currentTape = activeTape ?: Cassette(
        id = "tape_default",
        title = if (isRecording) "Radioaufnahme" else "Mein Mixtape",
        notes = "Radioaufnahme 1LIVE",
        colorHex = "#212328"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val availableHeight = maxHeight
        val isCompactHeight = availableHeight < 400.dp

        // 1. Äußeres Chassis: Exakt silber gebürstetes Aluminium mit feinen Lichtkanten
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .shadow(14.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3F6FA),
                            Color(0xFFE4E9F2),
                            Color(0xFFD5DCE8),
                            Color(0xFFCAD1DF),
                            Color(0xFFE8ECF5),
                            Color(0xFFBCC4D2),
                            Color(0xFFCBD2DE),
                            Color(0xFFE8EDF6)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFCBD2DE),
                            Color(0xFF7E8896),
                            Color(0xFFBAC2D0)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            // Feine Bürstungs-Textur (Hairline Brushed Aluminum Finish)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val step = 3.dp.toPx()
                var y = 0f
                var alt = false
                while (y < h) {
                    val lineColor = if (alt) Color.White.copy(alpha = 0.05f) else Color(0xFF6B7280).copy(alpha = 0.04f)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += step
                    alt = !alt
                }
            }

            // 2. Haupt-Layout: Schmale Aluminium-Lautsprecherleisten flankieren das zentrale Bedienfeld
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A. LINKER LAUTSPRECHER: Schmal mit gebohrten Löchern direkt im silbernen Aluminium
                NarrowPerforatedAluminumSpeaker(
                    label = "LEFT",
                    isLeft = true,
                    modifier = Modifier
                        .widthIn(min = 44.dp, max = 64.dp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                )

                // B. ZENTRALES BEDIENFELD (Silbernes Aluminium-Deck)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 5.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. OBERE KOPFLEISTE: Graviertes Marken-Typenschild, Status-LEDs und zylindrische Silberknöpfe
                    SilverBoomboxTopHeader(
                        isKeepScreenOn = isKeepScreenOn,
                        onToggleKeepScreenOn = onToggleKeepScreenOn,
                        onOpenStations = onOpenStations,
                        onOpenSettings = onOpenSettings,
                        onOpenShelf = onOpenShelf,
                        onOpenDetail = if (activeTape != null && onOpenDetail != null) {
                            { onOpenDetail(activeTape) }
                        } else null,
                        isRadioPlaying = isRadioPlaying,
                        isRecording = isRecording,
                        isTapeRunning = isTapeRunning,
                        isCompact = isCompactHeight
                    )

                    // 2. MITTLERER BEREICH: Links Kassettenfach (Silber) | Rechts Radiotuner (LCD & FM-Skala)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Linke Bucht: Authentisches Kassettenfach im Silber-Aluminium-Stil
                        Box(
                            modifier = Modifier.weight(1.08f),
                            contentAlignment = Alignment.Center
                        ) {
                            CassetteDoorCompartment(
                                cassette = currentTape,
                                isSpinning = isSpinning,
                                isRewinding = isRewinding,
                                isFastForwarding = isFastForwarding,
                                isRecording = isRecording,
                                isEjected = isEjected,
                                tapePositionSec = tapePositionSec,
                                totalCapacitySec = currentTape.totalCapacitySec,
                                onLabelClick = { onOpenCustomizer(currentTape) },
                                onOpenCustomizer = onOpenCustomizer,
                                onOpenDetail = onOpenDetail,
                                onInsertTape = onInsertTape,
                                isSilverStyle = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Rechte Bucht: Radiotuner (Digital LCD, analoge FM-Skala, Frequenz-Slider)
                        Column(
                            modifier = Modifier.weight(0.92f),
                            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 2.dp else 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Digitales LCD/VFD Radio-Display
                            DigitalRadioPanel(
                                station = currentStation,
                                nowPlayingText = nowPlaying,
                                isRecording = isRecording,
                                isFavorite = isCurrentStationFavorite,
                                isDimmed = isTapeMode,
                                isTapeRunning = isTapeRunning,
                                isRadioPlaying = isRadioPlaying,
                                onToggleRadioPlayback = onToggleRadioPlayback,
                                onPanelClick = {
                                    if (isTapeMode) {
                                        onSourceChanged(PlaybackSource.RADIO)
                                    } else {
                                        onOpenStations()
                                    }
                                },
                                onToggleFavorite = onToggleFavorite,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Analoge beleuchtete FM-Frequenzskala
                            TuningScale(
                                currentFrequencyMhz = tunedFrequency,
                                onFrequencyChange = onFrequencyChange,
                                isDimmed = isTapeMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCompactHeight) 38.dp else 44.dp)
                            )

                            // Taktiler FM-Frequenz-Schieberegler
                            FrequencySlider(
                                currentFrequencyMhz = tunedFrequency,
                                onFrequencyChange = onFrequencyChange,
                                isDimmed = isTapeMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCompactHeight) 30.dp else 34.dp)
                            )
                        }
                    }

                    // 3. UNTERE BEDIENLEISTE: 6 silberne Klaviaturtasten, Quellenschalter, Bandzähler & VU-Meter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 6 mechanische Piano-Transporttasten (silber gefräst)
                        Box(
                            modifier = Modifier.weight(1.08f)
                        ) {
                            TransportControls(
                                deckState = deckState,
                                onRecordClick = onRecordPress,
                                onPlayClick = onPlayPress,
                                onRewindClick = onRewindPress,
                                onFastForwardClick = onFastForwardPress,
                                onPauseClick = onPausePress,
                                onEjectClick = onStopEjectPress,
                                isSilverStyle = true,
                                isCueReviewActive = isCueReviewActive
                            )
                        }

                        // Quellenschalter & mechanisches 3-stelliges Zählwerk
                        Row(
                            modifier = Modifier
                                .weight(0.52f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE2E7F0), Color(0xFFCAD1DE), Color(0xFFBAC2D0))
                                    )
                                )
                                .border(1.dp, Color(0xFF8B95A5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FunctionSelectorSwitch(
                                currentSource = playbackSource,
                                onSourceChanged = onSourceChanged
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "COUNTER",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C323D),
                                    letterSpacing = 0.5.sp
                                )
                                MechanicalTapeCounter(
                                    value = counter,
                                    onReset = onResetCounter
                                )
                            }
                        }

                        // Stereo Dual VU-Meter
                        Column(
                            modifier = Modifier
                                .weight(0.40f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE2E7F0), Color(0xFFCAD1DE), Color(0xFFBAC2D0))
                                    )
                                )
                                .border(1.dp, Color(0xFF8B95A5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VU LEVEL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C323D),
                                letterSpacing = 0.5.sp
                            )
                            DualVuMeter(
                                levelLeft = vuLeft,
                                levelRight = vuRight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // C. RECHTER LAUTSPRECHER: Schmal mit gebohrten Löchern direkt im silbernen Aluminium
                NarrowPerforatedAluminumSpeaker(
                    label = "RIGHT",
                    isLeft = false,
                    modifier = Modifier
                        .widthIn(min = 44.dp, max = 64.dp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                )
            }
        }
    }
}

/**
 * Schmale Lautsprecherleiste mit direkt ins Aluminium gestanzten/gebohrten Lautsprecherlöchern:
 * - Gefertigt aus silber gebürstetem Aluminium passend zum gesamten Gehäuse
 * - Präzise Matrix aus feinen, kreisrunden Bohrungen (akustisch hinterlegt in Anthrazit/Schwarz)
 * - Jedes Loch besitzt eine echte 3D-Lichtkante (untere Glanzkante, oberer Innenschatten)
 * - Keine breiten Lautsprecher oder runden Konusse
 */
@Composable
fun NarrowPerforatedAluminumSpeaker(
    label: String,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE4E9F2),
                        Color(0xFFD3DBE7),
                        Color(0xFFC4CBD7),
                        Color(0xFFD6DCE7),
                        Color(0xFFBCC4D2)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFF8B95A5), Color(0xFFCBD2DE))
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Geätzte Kanalbeschriftung oben ("L" / "R" bzw. "LEFT" / "RIGHT")
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF374151),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )

            // Perforierte Aluminium-Lochmatrix (Lautsprecherlöcher im Aluminium)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val columns = 5
                val holeRadius = 2.0.dp.toPx()
                val holeSpacingX = w / (columns + 1)
                val holeSpacingY = 5.2.dp.toPx()
                val rows = ((h - 4.dp.toPx()) / holeSpacingY).toInt()

                val startY = 4.dp.toPx()

                for (r in 0 until rows) {
                    val y = startY + r * holeSpacingY
                    // Versetzte Wabenstruktur für perfekte Vintage-HiFi-Optik
                    val rowOffset = if (r % 2 == 1) holeSpacingX * 0.5f else 0f

                    for (c in 1..columns) {
                        val x = c * holeSpacingX - (holeSpacingX * 0.25f) + rowOffset
                        if (x in (holeRadius + 1.dp.toPx())..(w - holeRadius - 1.dp.toPx())) {
                            val holeCenter = Offset(x, y)

                            // 1. Untere helle Glanzlicht-Kante der Aluminiumbohrung
                            drawCircle(
                                color = Color.White.copy(alpha = 0.75f),
                                radius = holeRadius + 0.6.dp.toPx(),
                                center = Offset(x + 0.3.dp.toPx(), y + 0.4.dp.toPx())
                            )

                            // 2. Oberer Innenschatten der Bohrungskante
                            drawCircle(
                                color = Color(0xFF4B5565),
                                radius = holeRadius,
                                center = Offset(x - 0.2.dp.toPx(), y - 0.2.dp.toPx())
                            )

                            // 3. Dunkle akustische Kammeröffnung hinter dem Aluminium
                            drawCircle(
                                color = Color(0xFF0F1116),
                                radius = holeRadius,
                                center = holeCenter
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Silberne Kopfleiste im gebürsteten Aluminium-Stil:
 * - Vintage Hi-Fi-Stereo-Schriftzug in Schwarz/Anthrazit
 * - Juwel-LEDs: POWER (Rot), STEREO (Grün), REC (Rot)
 * - Zylindrische Aluminium-Drucktasten (Display anlassen, Radio, Einstellungen, Kassettensammlung, Info)
 */
@Composable
fun SilverBoomboxTopHeader(
    isKeepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onOpenStations: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenDetail: (() -> Unit)?,
    isRadioPlaying: Boolean,
    isRecording: Boolean,
    isTapeRunning: Boolean,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Linke Seite: Geätztes Typenschild & Status-LEDs
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Geätztes Aluminium-Typenschild
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E7F0), Color(0xFFCBD2DE))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFF8B95A5))),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "STEREO CASSETTE RECORDER",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E242E),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "• FM-AM 2-BAND",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2410C),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Echte LED-Statusanzeigen mit verchromter Fassung (POWER, STEREO, REC)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SilverLedIndicator(label = "POWER", isActive = true, activeColor = Color(0xFFFF2A1A))
                SilverLedIndicator(label = "STEREO", isActive = isRadioPlaying, activeColor = Color(0xFF00E676))
                SilverLedIndicator(label = "REC", isActive = isRecording, activeColor = Color(0xFFFF1744))
            }
        }

        // Rechte Seite: Zylindrische Silber-Bedienelemente
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Display dauerhaft anlassen (Keep Screen On)
            SilverActionButton(
                isHighlighted = isKeepScreenOn,
                highlightColor = Color(0xFFFFC107),
                onClick = onToggleKeepScreenOn,
                contentDescription = "Display dauerhaft an",
                isCompact = isCompact
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = null,
                    tint = if (isKeepScreenOn) Color(0xFFFFB300) else Color(0xFF333E4D),
                    modifier = Modifier.size(17.dp)
                )
            }

            // 2. Radiosender durchsuchen
            SilverActionButton(
                isHighlighted = false,
                onClick = onOpenStations,
                contentDescription = "Radiosender",
                isCompact = isCompact
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = Color(0xFF2C3542),
                    modifier = Modifier.size(17.dp)
                )
            }

            // 3. Einstellungen & Radio-Format auswählen
            SilverActionButton(
                isHighlighted = false,
                onClick = onOpenSettings,
                contentDescription = "Einstellungen & Radio-Format",
                isCompact = isCompact
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF2C3542),
                    modifier = Modifier.size(17.dp)
                )
            }

            // 4. Kassettensammlung
            SilverActionButton(
                isHighlighted = false,
                onClick = onOpenShelf,
                contentDescription = "Kassettensammlung",
                isCompact = isCompact
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = Color(0xFF92400E),
                    modifier = Modifier.size(17.dp)
                )
            }

            // 5. Kassetten-Details (falls vorhanden)
            if (onOpenDetail != null) {
                SilverActionButton(
                    isHighlighted = false,
                    onClick = onOpenDetail,
                    contentDescription = "Audiodatei-Informationen",
                    isCompact = isCompact
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2C3542),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Taktiler zylindrischer Knopf aus gebürstetem Silber-Aluminium
 */
@Composable
fun SilverActionButton(
    isHighlighted: Boolean,
    highlightColor: Color = Color(0xFFFFB300),
    onClick: () -> Unit,
    contentDescription: String,
    isCompact: Boolean,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isCompact) 32.dp else 34.dp)
            .shadow(2.5.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isHighlighted) {
                        listOf(Color(0xFFFFF3CD), Color(0xFFFFE082), Color(0xFFFFB300))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFE4E9F2), Color(0xFFBAC2CF))
                    }
                )
            )
            .border(
                1.dp,
                if (isHighlighted) highlightColor else Color(0xFF8B95A5),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            icon()
        }
    }
}

/**
 * LED-Statusanzeige mit Silberfassung
 */
@Composable
fun SilverLedIndicator(
    label: String,
    isActive: Boolean,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else Color(0xFF788394))
                .border(0.6.dp, Color(0xFFFFFFFF), CircleShape)
        )
        Text(
            text = label,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color(0xFF1E242E) else Color(0xFF6B7280)
        )
    }
}
