package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.BnetzaFmDatabase
import com.example.data.BnetzaRegion
import com.example.data.BnetzaStation
import com.example.data.FmDialMode
import com.example.data.FrequencyTuningManager
import com.example.data.LocationSelectionMethod
import com.example.model.RadioLayoutMode
import com.example.model.RadioStation
import com.example.model.RecordingMode
import com.example.ui.theme.ShareTechMonoFontFamily

enum class SettingsSubmenu {
    RADIO_MODEL,        // Radio auswählen
    FREQUENCY_TUNING,   // Frequenzenstellung
    RECORDING_MODE      // MP3 Aufnahme
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentRecordingMode: RecordingMode,
    currentLayoutMode: RadioLayoutMode,
    onRecordingModeSelect: (RecordingMode) -> Unit,
    onRadioLayoutModeSelect: (RadioLayoutMode) -> Unit,
    onDismiss: () -> Unit,
    frequencyTuningManager: FrequencyTuningManager? = null,
    currentTunedStation: RadioStation? = null,
    onTuneStation: ((RadioStation) -> Unit)? = null,
    initialSubmenu: SettingsSubmenu = SettingsSubmenu.RADIO_MODEL,
    isCueReviewAudioEnabled: Boolean = true,
    onCueReviewAudioEnabledChange: ((Boolean) -> Unit)? = null,
    isFullscreenEnabled: Boolean = false,
    onFullscreenEnabledChange: ((Boolean) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeSubmenu by remember { mutableStateOf(initialSubmenu) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF191C22),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Header: "Einstellungen" & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Einstellungen",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Hi-Fi Studio & Kassettenrekorder",
                        fontSize = 12.sp,
                        color = Color(0xFF8B95A5)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vollbildmodus-Schalter (Immersive Display: Benachrichtigungs- & Navigationsleiste ausblenden)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14171D)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isFullscreenEnabled) Color(0xFFFFB300).copy(alpha = 0.6f) else Color(0xFF2C323D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFullscreenEnabled) Color(0xFF332612) else Color(0xFF20242D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFullscreenEnabled) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = if (isFullscreenEnabled) Color(0xFFFFB300) else Color(0xFF8B95A5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vollbildmodus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Status- & Navigationsleiste ausblenden",
                                fontSize = 11.5.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    Switch(
                        checked = isFullscreenEnabled,
                        onCheckedChange = { onFullscreenEnabledChange?.invoke(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFB300),
                            checkedTrackColor = Color(0xFF4A3816),
                            uncheckedThumbColor = Color(0xFFB0BEC5),
                            uncheckedTrackColor = Color(0xFF262C36)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Submenu Tabs (Segmented Navigation - 3 Submenüs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF101216))
                    .border(1.dp, Color(0xFF2C323D), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 1: Radio auswählen
                val isRadioTab = activeSubmenu == SettingsSubmenu.RADIO_MODEL
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isRadioTab) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF323A48), Color(0xFF222832))
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isRadioTab) 1.dp else 0.dp,
                            color = if (isRadioTab) Color(0xFFFFB300) else Color.Transparent,
                            shape = RoundedCornerShape(7.dp)
                        )
                        .clickable { activeSubmenu = SettingsSubmenu.RADIO_MODEL }
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = if (isRadioTab) Color(0xFFFFB300) else Color(0xFF8B95A5),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Radio-Design",
                        fontSize = 11.5.sp,
                        fontWeight = if (isRadioTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRadioTab) Color.White else Color(0xFF8B95A5)
                    )
                }

                // Tab 2: Frequenzenstellung
                val isFreqTab = activeSubmenu == SettingsSubmenu.FREQUENCY_TUNING
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isFreqTab) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF323A48), Color(0xFF222832))
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isFreqTab) 1.dp else 0.dp,
                            color = if (isFreqTab) Color(0xFFFFB300) else Color.Transparent,
                            shape = RoundedCornerShape(7.dp)
                        )
                        .clickable { activeSubmenu = SettingsSubmenu.FREQUENCY_TUNING }
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (isFreqTab) Color(0xFFFFB300) else Color(0xFF8B95A5),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Frequenzen",
                        fontSize = 11.5.sp,
                        fontWeight = if (isFreqTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFreqTab) Color.White else Color(0xFF8B95A5)
                    )
                }

                // Tab 3: MP3 Aufnahme
                val isRecTab = activeSubmenu == SettingsSubmenu.RECORDING_MODE
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isRecTab) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF323A48), Color(0xFF222832))
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isRecTab) 1.dp else 0.dp,
                            color = if (isRecTab) Color(0xFFFFB300) else Color.Transparent,
                            shape = RoundedCornerShape(7.dp)
                        )
                        .clickable { activeSubmenu = SettingsSubmenu.RECORDING_MODE }
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecTab) Color(0xFFFFB300) else Color(0xFF8B95A5),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aufnahme",
                        fontSize = 11.5.sp,
                        fontWeight = if (isRecTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRecTab) Color.White else Color(0xFF8B95A5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submenu Content
            AnimatedContent(
                targetState = activeSubmenu,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "submenuTransition"
            ) { submenu ->
                when (submenu) {
                    SettingsSubmenu.RADIO_MODEL -> {
                        RadioModelSelectionSubmenu(
                            currentLayoutMode = currentLayoutMode,
                            onSelect = onRadioLayoutModeSelect
                        )
                    }

                    SettingsSubmenu.FREQUENCY_TUNING -> {
                        val ctx = LocalContext.current
                        val mgr = frequencyTuningManager ?: remember { FrequencyTuningManager.getInstance(ctx) }
                        FrequencyTuningSubmenu(
                            manager = mgr,
                            currentTunedStation = currentTunedStation,
                            onTuneStation = onTuneStation
                        )
                    }

                    SettingsSubmenu.RECORDING_MODE -> {
                        RecordingOptionsSubmenu(
                            currentMode = currentRecordingMode,
                            onModeSelect = onRecordingModeSelect,
                            isCueReviewAudioEnabled = isCueReviewAudioEnabled,
                            onCueReviewAudioEnabledChange = onCueReviewAudioEnabledChange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Submenu: Radio auswählen (Hochformat vs. Querformat) with authentic preview images
 */
@Composable
fun RadioModelSelectionSubmenu(
    currentLayoutMode: RadioLayoutMode,
    onSelect: (RadioLayoutMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Wähle das gewünschte Radio-Modell & Format:",
            fontSize = 14.sp,
            color = Color(0xFFCFD8DC)
        )

        // Option 1: Hochformat (Klassisches Kassettendeck Anthrazit)
        val isPortrait = currentLayoutMode == RadioLayoutMode.PORTRAIT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPortrait) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isPortrait) 2.dp else 1.dp,
                    color = if (isPortrait) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect(RadioLayoutMode.PORTRAIT) }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isPortrait,
                        onClick = { onSelect(RadioLayoutMode.PORTRAIT) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                    )
                    Column {
                        Text(
                            text = "Klassisches Deck (Anthrazit)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dunkles Studio-Rack für vertikale Haltung",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                if (isPortrait) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3D2E0B))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AKTIV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Preview Graphic for Portrait Deck
            PortraitDeckPreviewGraphic(
                isSelected = isPortrait,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Schlankes, edles Hi-Fi Kassettendeck im Hochformat mit dunkler Studio-Front. Einhändige Bedienung mit voller Bandmechanik, Sichtfenster und Skala.",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }

        // Option 2: Hochformat (Silber gebürstetes Aluminium) - NEUES 3. DESIGN!
        val isPortraitAluminum = currentLayoutMode == RadioLayoutMode.PORTRAIT_ALUMINUM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPortraitAluminum) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isPortraitAluminum) 2.dp else 1.dp,
                    color = if (isPortraitAluminum) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect(RadioLayoutMode.PORTRAIT_ALUMINUM) }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isPortraitAluminum,
                        onClick = { onSelect(RadioLayoutMode.PORTRAIT_ALUMINUM) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                    )
                    Column {
                        Text(
                            text = "Aluminium-Deck (Hochformat)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Edles, silber gebürstetes Aluminium",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                if (isPortraitAluminum) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3D2E0B))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AKTIV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Preview Graphic for Portrait Aluminum Deck
            PortraitAluminumPreviewGraphic(
                isSelected = isPortraitAluminum,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vertikaler Kassettenradioplayer in exakt demselben silber gebürsteten Aluminium wie beim Quer-Modell: Gefräste CNC-Lichtkanten, extrudierte Aluminium-Tasten, Rubin-Rec-Taste und silberne Kassettenblende.",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }

        // Option 3: Querformat (Hi-Fi Radiorekorder / Ghettoblaster)
        val isLandscape = currentLayoutMode == RadioLayoutMode.LANDSCAPE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isLandscape) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isLandscape) 2.dp else 1.dp,
                    color = if (isLandscape) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect(RadioLayoutMode.LANDSCAPE) }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isLandscape,
                        onClick = { onSelect(RadioLayoutMode.LANDSCAPE) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                    )
                    Column {
                        Text(
                            text = "Stereo Radiorekorder (Querformat)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Klassische Ghettoblaster-Boombox mit Stereo-Boxen",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                if (isLandscape) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3D2E0B))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AKTIV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Preview Graphic for Landscape Boombox
            LandscapeBoomboxPreviewGraphic(
                isSelected = isLandscape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Klassischer Ghettoblaster im Breitformat exakt wie auf deinem Foto: Große Lautsprecher links & rechts, Cassettenfach, analoge Skala und VU-Meter.",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }

        // Option 4: Vintage Transistorradio (Querformat - ohne Kassettendeck)
        val isTransistor = currentLayoutMode == RadioLayoutMode.TRANSISTOR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isTransistor) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isTransistor) 2.dp else 1.dp,
                    color = if (isTransistor) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect(RadioLayoutMode.TRANSISTOR) }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isTransistor,
                        onClick = { onSelect(RadioLayoutMode.TRANSISTOR) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                    )
                    Column {
                        Text(
                            text = "Vintage Transistorradio (Querformat)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Klassisches Tischradio ohne Kassettendeck wie im Foto",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                if (isTransistor) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3D2E0B))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AKTIV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Preview Graphic for Transistor Radio
            TransistorRadioPreviewGraphic(
                isSelected = isTransistor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Exakt wie im Foto: Gehäuse mit dunkler Ledertextur und Chromrahmen, Teleskopantenne, Lochgitter-Lautsprecher, bernsteinfarbene FM-Skala mit Schieberegler, FM/AM/OFF-Schalter und drehbares Volumerad (dreht sich bei Smartphonetasten mit).",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }

        // Option 5: Retro Transistorradio Cremeweiß (Querformat mit Stoffbespannung)
        val isCreamTransistor = currentLayoutMode == RadioLayoutMode.TRANSISTOR_CREAM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCreamTransistor) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isCreamTransistor) 2.dp else 1.dp,
                    color = if (isCreamTransistor) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect(RadioLayoutMode.TRANSISTOR_CREAM) }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isCreamTransistor,
                        onClick = { onSelect(RadioLayoutMode.TRANSISTOR_CREAM) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                    )
                    Column {
                        Text(
                            text = "Transistorradio Cremeweiß (Querformat)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Retro-Design mit edler Stoffbespannung",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                if (isCreamTransistor) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3D2E0B))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AKTIV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realistic Preview Graphic for Cream Transistor Radio
            CreamTransistorRadioPreviewGraphic(
                isSelected = isCreamTransistor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Exakt wie im Foto: Cremefarbenes Retro-Gehäuse mit sanften Kantenfasen, lederner Trageriemen, dahinterliegende Teleskopantenne, ON/OFF-Schiebeschalter mit Kontroll-LED, bernsteinfarbenes Skalenmodul mit Schieberegler, konzentrisch gebürstetes Aluminium-Volumerad (mit Hardwaretasten synchronisiert) und detailverliebter gewebter Akustikstoff über die gesamte Gehäusebreite.",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Submenu: MP3-Aufnahme (Voller Mitschnitt vs. einzelne Clips)
 */
@Composable
fun RecordingOptionsSubmenu(
    currentMode: RecordingMode,
    onModeSelect: (RecordingMode) -> Unit,
    isCueReviewAudioEnabled: Boolean = true,
    onCueReviewAudioEnabledChange: ((Boolean) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Lege fest, wie Radioaufnahmen auf Kassette gespeichert werden:",
            fontSize = 14.sp,
            color = Color(0xFFCFD8DC)
        )

        // Option 1: Voller Mitschnitt (empfohlen)
        val isContinuous = currentMode == RecordingMode.CONTINUOUS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isContinuous) Color(0xFF262A33) else Color(0xFF16181D))
                .border(
                    width = if (isContinuous) 1.5.dp else 1.dp,
                    color = if (isContinuous) Color(0xFFFFB300) else Color(0xFF37474F),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onModeSelect(RecordingMode.CONTINUOUS) }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                RadioButton(
                    selected = isContinuous,
                    onClick = { onModeSelect(RecordingMode.CONTINUOUS) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voller Mitschnitt (empfohlen)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Komplette Aufnahme als eine zusammenhängende Audiodatei mit Metadaten der erkannten Songs. Wie bei einer echten Kassette.",
                        fontSize = 13.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            WaveformGraphic(isSegmented = false)
        }

        // Option 2: Automatisch in einzelne Clips schneiden
        val isClips = currentMode == RecordingMode.CLIPS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isClips) Color(0xFF262A33) else Color(0xFF16181D))
                .border(
                    width = if (isClips) 1.5.dp else 1.dp,
                    color = if (isClips) Color(0xFFFFB300) else Color(0xFF37474F),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onModeSelect(RecordingMode.CLIPS) }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                RadioButton(
                    selected = isClips,
                    onClick = { onModeSelect(RecordingMode.CLIPS) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Automatisch in einzelne Clips schneiden",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Songs werden automatisch erkannt und als separate Audiodateien mit Trackmarkern gespeichert.",
                        fontSize = 13.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            WaveformGraphic(isSegmented = true)
        }

        // Option 3: Kassetten-Wiedergabe: Cue / Review Schnelllauf-Audio
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kassetten-Wiedergabe & Bandlauf:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCFD8DC)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF16181D))
                .border(1.dp, Color(0xFF37474F), RoundedCornerShape(10.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cue / Review Mithören",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "3x SPEED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spielt beim Drücken von Vor- (CUE) oder Rücklauf (REVIEW) während PLAY die echte Aufnahme mit 3-facher Geschwindigkeit und analogem Micky-Maus-Sound ab. Bei STOP bleibt der Schnelllauf stumm.",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5)
                )
            }
            Switch(
                checked = isCueReviewAudioEnabled,
                onCheckedChange = { onCueReviewAudioEnabledChange?.invoke(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFB300),
                    checkedTrackColor = Color(0xFF37474F),
                    uncheckedThumbColor = Color(0xFF90A4AE),
                    uncheckedTrackColor = Color(0xFF1B202A)
                )
            )
        }
    }
}

/**
 * Submenu: Frequenzenstellung
 * Ermöglicht die Wahl zwischen Favoriten-Bandbelegung und echten UKW/FM-Sendern der Bundesnetzagentur nach Ort/GPS.
 * Zeigt die vor Ort verfügbaren Radiosender mit exakter FM-Frequenz (MHz), Sendeleistung und Senderstandort an.
 */
@Composable
fun FrequencyTuningSubmenu(
    manager: FrequencyTuningManager,
    currentTunedStation: RadioStation? = null,
    onTuneStation: ((RadioStation) -> Unit)? = null
) {
    val context = LocalContext.current
    val fmMode by manager.fmDialMode.collectAsState()
    val locationMethod by manager.locationMethod.collectAsState()
    val activeRegion by manager.activeRegion.collectAsState()
    val gpsInfo by manager.gpsLocationInfo.collectAsState()
    val isGpsLocating by manager.isGpsLocating.collectAsState()
    val isFmMuting by manager.isFmMutingEnabled.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            manager.performGpsLocation(context) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Standortberechtigung wurde nicht erteilt", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Skalenband-Belegung (FM 87.5 – 108.0 MHz):",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCFD8DC)
        )

        // 1. Modus-Wahl: Favoriten vs. Echte FM-Sender (BNetzA)
        // Option A: Favoriten auf dem Band
        val isFavorites = fmMode == FmDialMode.FAVORITES
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFavorites) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isFavorites) 1.5.dp else 1.dp,
                    color = if (isFavorites) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { manager.setFmDialMode(FmDialMode.FAVORITES) }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isFavorites,
                    onClick = { manager.setFmDialMode(FmDialMode.FAVORITES) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Favoriten auf dem Band",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Deine persönlichen Lieblingssender werden auf der analogen FM-Skala abgebildet.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }
        }

        // Option B: Echte FM-Radiosender nach Ort/GPS (BNetzA)
        val isBnetza = fmMode == FmDialMode.LOCATION_BNETZA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isBnetza) Color(0xFF242933) else Color(0xFF14161C))
                .border(
                    width = if (isBnetza) 1.5.dp else 1.dp,
                    color = if (isBnetza) Color(0xFFFFB300) else Color(0xFF333A47),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { manager.setFmDialMode(FmDialMode.LOCATION_BNETZA) }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isBnetza,
                    onClick = { manager.setFmDialMode(FmDialMode.LOCATION_BNETZA) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Echte FM-Sender nach Ort / GPS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Bundesnetzagentur Open Data: Reale UKW-Frequenzen lokaler Sender am Standort.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }
        }

        // 2. UKW-Zwischenrauschen / FM-Muting (Rauschsperre)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF161920))
                .border(1.dp, Color(0xFF2A313E), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FM-Muting (Rauschsperre)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFmMuting) {
                            "Aktiv: Sendelücken auf der Skala bleiben stumm (kein Rauschen)."
                        } else {
                            "Aus: Authentisches analoges UKW-Zwischenstationsrauschen beim Suchen."
                        },
                        fontSize = 11.sp,
                        color = if (!isFmMuting) Color(0xFFFFB300) else Color(0xFF90A4AE),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = isFmMuting,
                    onCheckedChange = { manager.setFmMutingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB300),
                        checkedTrackColor = Color(0xFF5D400A),
                        uncheckedThumbColor = Color(0xFF78909C),
                        uncheckedTrackColor = Color(0xFF263238)
                    )
                )
            }
        }

        // 3. Standort-Einstellungen (wenn BNetzA-Modus aktiv)
        if (isBnetza) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161920))
                    .border(1.dp, Color(0xFF2A313E), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Standort-Ermittlung:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFC107)
                )

                // Umschalter: GPS vs. Manuell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1116))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isGpsSelected = locationMethod == LocationSelectionMethod.GPS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGpsSelected) Color(0xFF2C3442) else Color.Transparent)
                            .clickable {
                                manager.setLocationMethod(LocationSelectionMethod.GPS)
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasCoarse || hasFine) {
                                    manager.performGpsLocation(context) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isGpsSelected) Color(0xFFFFC107) else Color(0xFF8B95A5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "GPS-Ortung",
                                fontSize = 12.sp,
                                fontWeight = if (isGpsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isGpsSelected) Color.White else Color(0xFF8B95A5)
                            )
                        }
                    }

                    val isManualSelected = locationMethod == LocationSelectionMethod.MANUAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isManualSelected) Color(0xFF2C3442) else Color.Transparent)
                            .clickable { manager.setLocationMethod(LocationSelectionMethod.MANUAL) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isManualSelected) Color(0xFFFFC107) else Color(0xFF8B95A5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Ort manuell wählen",
                                fontSize = 12.sp,
                                fontWeight = if (isManualSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isManualSelected) Color.White else Color(0xFF8B95A5)
                            )
                        }
                    }
                }

                if (locationMethod == LocationSelectionMethod.GPS) {
                    // GPS Trigger Button & Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF202631))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aktiver GPS-Status:",
                                fontSize = 11.sp,
                                color = Color(0xFF8B95A5)
                            )
                            Text(
                                text = gpsInfo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300))
                                .clickable(enabled = !isGpsLocating) {
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasCoarse || hasFine) {
                                        manager.performGpsLocation(context) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGpsLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Ortung starten",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                } else {
                    // Manuelle Ortsauswahl (Region Chips)
                    Text(
                        text = "Wähle deine Senderegion aus:",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5)
                    )

                    val allRegions = remember { BnetzaFmDatabase.getAllRegions() }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allRegions.chunked(2).forEach { rowRegions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowRegions.forEach { region ->
                                    val isSelected = region.id == activeRegion.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF333C4A) else Color(0xFF1E222B))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFFFB300) else Color(0xFF374151),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { manager.selectRegion(region.id) }
                                            .padding(vertical = 7.dp, horizontal = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = region.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFFFFC107) else Color.White
                                                )
                                                Text(
                                                    text = "${region.stations.size} Sender",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF8B95A5)
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowRegions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 3. ANZEIGE DER VOR ORT VERFÜGBAREN RADIOSENDER (MIT FREQUENZ IN MHZ)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF13151A))
                    .border(1.dp, Color(0xFF2C323D), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Verfügbare Sender: ${activeRegion.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Senderstandort: ${activeRegion.transmitterHubName}",
                            fontSize = 11.sp,
                            color = Color(0xFF8B95A5)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${activeRegion.stations.size} UKW-Sender",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Liste aller koordinierten FM-Sender vor Ort mit Frequenz (MHz)
                activeRegion.stations.forEach { station ->
                    val isTuned = currentTunedStation?.dialFrequencyMhz == station.frequencyMhz ||
                            currentTunedStation?.name == station.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTuned) Color(0xFF29323E) else Color(0xFF1B1E26))
                            .border(
                                width = if (isTuned) 1.5.dp else 1.dp,
                                color = if (isTuned) Color(0xFFFFB300) else Color(0xFF2B3340),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onTuneStation?.invoke(station.toRadioStation())
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // FM Frequenz Badge (MHz)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF0B0D11))
                                .border(1.dp, Color(0xFF374151), RoundedCornerShape(5.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f MHz", station.frequencyMhz),
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Sendername & BNetzA Daten
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${station.transmitterLocation} • ${station.erpKw}",
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE)
                            )
                            Text(
                                text = station.category,
                                fontSize = 10.sp,
                                color = Color(0xFF607D8B)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status / Direktwahl
                        if (isTuned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1B5E20))
                                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "EINGESTELLT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2A323F))
                                    .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(4.dp))
                                    .clickable {
                                        onTuneStation?.invoke(station.toRadioStation())
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Wählen",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview Graphic: Portrait Deck (Klassisches Deck)
 */
@Composable
fun PortraitDeckPreviewGraphic(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0E12))
            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0xFF2C323D), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2

        // Deck Body (Vertical box centered)
        val deckWidth = (h * 0.72f).coerceAtMost(w * 0.45f)
        val deckLeft = cx - deckWidth / 2
        val deckTop = 2f
        val deckBottom = h - 2f
        val deckHeight = deckBottom - deckTop

        // Chassis Outer Frame
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF383E4B), Color(0xFF21252D), Color(0xFF14161C)),
                startY = deckTop,
                endY = deckBottom
            ),
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckWidth, deckHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFFFFB300).copy(alpha = 0.8f) else Color(0xFF555E6F),
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckWidth, deckHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Upper section: Cassette door window
        val windowTop = deckTop + deckHeight * 0.10f
        val windowHeight = deckHeight * 0.36f
        val windowWidth = deckWidth * 0.85f
        val windowLeft = cx - windowWidth / 2

        drawRoundRect(
            color = Color(0xFF0B0D10),
            topLeft = Offset(windowLeft, windowTop),
            size = Size(windowWidth, windowHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Tape spools in window
        val reelRadius = windowHeight * 0.22f
        val reelY = windowTop + windowHeight / 2
        val leftReelX = windowLeft + windowWidth * 0.32f
        val rightReelX = windowLeft + windowWidth * 0.68f

        drawCircle(color = Color(0xFFD4AF37), radius = reelRadius, center = Offset(leftReelX, reelY))
        drawCircle(color = Color(0xFF1A1C20), radius = reelRadius * 0.5f, center = Offset(leftReelX, reelY))
        drawCircle(color = Color(0xFFD4AF37), radius = reelRadius, center = Offset(rightReelX, reelY))
        drawCircle(color = Color(0xFF1A1C20), radius = reelRadius * 0.5f, center = Offset(rightReelX, reelY))

        // Middle section: Amber FM Tuning Scale
        val scaleTop = deckTop + deckHeight * 0.52f
        val scaleHeight = deckHeight * 0.12f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2A1B07), Color(0xFF472D0B), Color(0xFF2A1B07)),
                startX = windowLeft,
                endX = windowLeft + windowWidth
            ),
            topLeft = Offset(windowLeft, scaleTop),
            size = Size(windowWidth, scaleHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Red tuning needle
        val needleX = windowLeft + windowWidth * 0.58f
        drawLine(
            color = Color(0xFFFF3D00),
            start = Offset(needleX, scaleTop),
            end = Offset(needleX, scaleTop + scaleHeight),
            strokeWidth = 2.dp.toPx()
        )

        // Lower middle section: Digital Radio panel simulation
        val lcdTop = deckTop + deckHeight * 0.68f
        val lcdHeight = deckHeight * 0.11f
        drawRoundRect(
            color = Color(0xFF09170D),
            topLeft = Offset(windowLeft, lcdTop),
            size = Size(windowWidth, lcdHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Tiny glowing segments
        drawLine(
            color = Color(0xFF00E676),
            start = Offset(windowLeft + 4.dp.toPx(), lcdTop + lcdHeight / 2),
            end = Offset(windowLeft + windowWidth * 0.7f, lcdTop + lcdHeight / 2),
            strokeWidth = 1.5.dp.toPx()
        )

        // Bottom section: 6 Piano Keys
        val keysTop = deckTop + deckHeight * 0.83f
        val keysHeight = deckHeight * 0.12f
        val keyCount = 6
        val keyWidth = (windowWidth - (keyCount - 1) * 2.dp.toPx()) / keyCount
        for (i in 0 until keyCount) {
            val kx = windowLeft + i * (keyWidth + 2.dp.toPx())
            drawRoundRect(
                color = if (i == 0) Color(0xFFB71C1C) else Color(0xFF788496),
                topLeft = Offset(kx, keysTop),
                size = Size(keyWidth, keysHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

/**
 * Preview Graphic: Portrait Aluminum Deck (Silber gebürstetes Aluminium)
 */
@Composable
fun PortraitAluminumPreviewGraphic(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0E12))
            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0xFF2C323D), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2

        // Deck Body (Vertical box centered)
        val deckWidth = (h * 0.72f).coerceAtMost(w * 0.45f)
        val deckLeft = cx - deckWidth / 2
        val deckTop = 2f
        val deckBottom = h - 2f
        val deckHeight = deckBottom - deckTop

        // Brushed Silver Aluminum Chassis Outer Frame
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF3F6FA),
                    Color(0xFFE4E9F2),
                    Color(0xFFD5DCE8),
                    Color(0xFFCAD1DF),
                    Color(0xFFBCC4D2),
                    Color(0xFFE8ECF5)
                ),
                startY = deckTop,
                endY = deckBottom
            ),
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckWidth, deckHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFFFFB300) else Color(0xFF8B95A5),
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckWidth, deckHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx())
        )

        // Hairline reflection highlight across the brushed aluminum body
        drawLine(
            color = Color(0x60FFFFFF),
            start = Offset(deckLeft + 1.dp.toPx(), deckTop + 1.dp.toPx()),
            end = Offset(deckLeft + deckWidth - 1.dp.toPx(), deckTop + 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // Upper section: Cassette door window with silver beveled frame
        val windowTop = deckTop + deckHeight * 0.10f
        val windowHeight = deckHeight * 0.36f
        val windowWidth = deckWidth * 0.85f
        val windowLeft = cx - windowWidth / 2

        // Outer silver bevel around window
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0B9C6)),
                startY = windowTop - 1.dp.toPx(),
                endY = windowTop + windowHeight + 1.dp.toPx()
            ),
            topLeft = Offset(windowLeft - 1.dp.toPx(), windowTop - 1.dp.toPx()),
            size = Size(windowWidth + 2.dp.toPx(), windowHeight + 2.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawRoundRect(
            color = Color(0xFF0B0D10),
            topLeft = Offset(windowLeft, windowTop),
            size = Size(windowWidth, windowHeight),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
        )

        // Tape spools in window
        val reelRadius = windowHeight * 0.22f
        val reelY = windowTop + windowHeight / 2
        val leftReelX = windowLeft + windowWidth * 0.32f
        val rightReelX = windowLeft + windowWidth * 0.68f

        drawCircle(color = Color(0xFFD4AF37), radius = reelRadius, center = Offset(leftReelX, reelY))
        drawCircle(color = Color(0xFF1A1C20), radius = reelRadius * 0.5f, center = Offset(leftReelX, reelY))
        drawCircle(color = Color(0xFFD4AF37), radius = reelRadius, center = Offset(rightReelX, reelY))
        drawCircle(color = Color(0xFF1A1C20), radius = reelRadius * 0.5f, center = Offset(rightReelX, reelY))

        // Middle section: Amber FM Tuning Scale with silver border
        val scaleTop = deckTop + deckHeight * 0.52f
        val scaleHeight = deckHeight * 0.12f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2A1B07), Color(0xFF472D0B), Color(0xFF2A1B07)),
                startX = windowLeft,
                endX = windowLeft + windowWidth
            ),
            topLeft = Offset(windowLeft, scaleTop),
            size = Size(windowWidth, scaleHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Red tuning needle
        val needleX = windowLeft + windowWidth * 0.58f
        drawLine(
            color = Color(0xFFFF3D00),
            start = Offset(needleX, scaleTop),
            end = Offset(needleX, scaleTop + scaleHeight),
            strokeWidth = 2.dp.toPx()
        )

        // Lower middle section: Digital Radio panel with emerald digital readout
        val lcdTop = deckTop + deckHeight * 0.68f
        val lcdHeight = deckHeight * 0.11f
        drawRoundRect(
            color = Color(0xFF09170D),
            topLeft = Offset(windowLeft, lcdTop),
            size = Size(windowWidth, lcdHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFF00E676),
            start = Offset(windowLeft + 4.dp.toPx(), lcdTop + lcdHeight / 2),
            end = Offset(windowLeft + windowWidth * 0.7f, lcdTop + lcdHeight / 2),
            strokeWidth = 1.5.dp.toPx()
        )

        // Bottom section: Piano Keys (Extruded Silver Aluminum with Ruby Rec Key)
        val keysTop = deckTop + deckHeight * 0.83f
        val keysHeight = deckHeight * 0.12f
        val keyCount = 6
        val keyWidth = (windowWidth - (keyCount - 1) * 2.dp.toPx()) / keyCount
        for (i in 0 until keyCount) {
            val kx = windowLeft + i * (keyWidth + 2.dp.toPx())
            drawRoundRect(
                color = if (i == 0) Color(0xFFC62828) else Color(0xFFFFFFFF),
                topLeft = Offset(kx, keysTop),
                size = Size(keyWidth, keysHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            // Delicate key bevel border
            drawRoundRect(
                color = if (i == 0) Color(0xFF8B1D23) else Color(0xFF8B95A5),
                topLeft = Offset(kx, keysTop),
                size = Size(keyWidth, keysHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                style = Stroke(width = 0.5.dp.toPx())
            )
        }
    }
}

/**
 * Preview Graphic: Landscape Boombox (Stereo Radiorekorder)
 */
@Composable
fun LandscapeBoomboxPreviewGraphic(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0E12))
            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0xFF2C323D), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val w = size.width
        val h = size.height

        // Boombox Body (Wide horizontal box)
        val boxWidth = (w - 8.dp.toPx()).coerceAtMost(h * 2.6f)
        val boxLeft = (w - boxWidth) / 2
        val boxTop = 4f
        val boxBottom = h - 4f
        val boxHeight = boxBottom - boxTop

        // Brushed Aluminum Chassis
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF3F6FA), Color(0xFFE4E9F2), Color(0xFFD5DCE8), Color(0xFFCAD1DF), Color(0xFFBCC4D2), Color(0xFFE8ECF5)),
                startY = boxTop,
                endY = boxBottom
            ),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFFFFB300) else Color(0xFFFFFFFF),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Top Handle Bar
        val handleW = boxWidth * 0.6f
        val handleLeft = boxLeft + (boxWidth - handleW) / 2
        drawRoundRect(
            color = Color(0xFF8B95A5),
            topLeft = Offset(handleLeft, boxTop - 2.dp.toPx()),
            size = Size(handleW, 3.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // 1. LEFT NARROW SPEAKER PANEL (Lautsprecherlöcher im Aluminium und schmal)
        val spkW = boxWidth * 0.12f
        val spkTop = boxTop + 4.dp.toPx()
        val spkH = boxHeight - 8.dp.toPx()
        val leftSpkX = boxLeft + 3.dp.toPx()
        val rightSpkX = boxLeft + boxWidth - spkW - 3.dp.toPx()

        // Draw Left Speaker Perforated Aluminum
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFE2E7F0), Color(0xFFCAD1DE))),
            topLeft = Offset(leftSpkX, spkTop),
            size = Size(spkW, spkH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Perforated hole matrix
        val cols = 3
        val rows = 12
        for (r in 0 until rows) {
            val hy = spkTop + 3.dp.toPx() + r * (spkH - 6.dp.toPx()) / rows
            for (c in 0 until cols) {
                val hx = leftSpkX + 2.dp.toPx() + c * (spkW - 4.dp.toPx()) / cols
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.2.dp.toPx(), center = Offset(hx + 0.3f, hy + 0.3f))
                drawCircle(color = Color(0xFF141720), radius = 1.0.dp.toPx(), center = Offset(hx, hy))
            }
        }

        // Draw Right Speaker Perforated Aluminum
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFE2E7F0), Color(0xFFCAD1DE))),
            topLeft = Offset(rightSpkX, spkTop),
            size = Size(spkW, spkH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        for (r in 0 until rows) {
            val hy = spkTop + 3.dp.toPx() + r * (spkH - 6.dp.toPx()) / rows
            for (c in 0 until cols) {
                val hx = rightSpkX + 2.dp.toPx() + c * (spkW - 4.dp.toPx()) / cols
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.2.dp.toPx(), center = Offset(hx + 0.3f, hy + 0.3f))
                drawCircle(color = Color(0xFF141720), radius = 1.0.dp.toPx(), center = Offset(hx, hy))
            }
        }

        // Center Console Panel (Silbernes Aluminium-Zentrum)
        val centerLeft = leftSpkX + spkW + 3.dp.toPx()
        val centerRight = rightSpkX - 3.dp.toPx()
        val centerWidth = centerRight - centerLeft
        val centerTop = boxTop + 4.dp.toPx()
        val centerHeight = boxHeight - 8.dp.toPx()

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFEFF3F8), Color(0xFFDCE2EC), Color(0xFFCAD2DF))),
            topLeft = Offset(centerLeft, centerTop),
            size = Size(centerWidth, centerHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Inside Center: Left = Cassette Door, Right = Tuning & Radio
        val halfCenterW = (centerWidth - 4.dp.toPx()) / 2

        // Left Half: Cassette Door
        val tapeLeft = centerLeft + 2.dp.toPx()
        val tapeTop = centerTop + 3.dp.toPx()
        val tapeHeight = centerHeight * 0.58f
        drawRoundRect(
            color = Color(0xFF0D0F14),
            topLeft = Offset(tapeLeft, tapeTop),
            size = Size(halfCenterW, tapeHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Two cassette reels
        val cReelR = tapeHeight * 0.22f
        val cReelY = tapeTop + tapeHeight / 2
        val cReel1X = tapeLeft + halfCenterW * 0.32f
        val cReel2X = tapeLeft + halfCenterW * 0.68f
        drawCircle(color = Color(0xFFD4AF37), radius = cReelR, center = Offset(cReel1X, cReelY))
        drawCircle(color = Color(0xFFD4AF37), radius = cReelR, center = Offset(cReel2X, cReelY))

        // Right Half: Radio LCD & Tuning Scale
        val radioLeft = centerLeft + halfCenterW + 2.dp.toPx()
        val radioTop = centerTop + 3.dp.toPx()
        val lcdH = tapeHeight * 0.44f
        drawRoundRect(
            color = Color(0xFF09170D),
            topLeft = Offset(radioLeft, radioTop),
            size = Size(halfCenterW, lcdH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFF00E676),
            start = Offset(radioLeft + 3.dp.toPx(), radioTop + lcdH / 2),
            end = Offset(radioLeft + halfCenterW * 0.8f, radioTop + lcdH / 2),
            strokeWidth = 1.5.dp.toPx()
        )

        val scaleTop = radioTop + lcdH + 2.dp.toPx()
        val scaleH = tapeHeight * 0.44f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2A1B07), Color(0xFF5A390E), Color(0xFF2A1B07)),
                startX = radioLeft,
                endX = radioLeft + halfCenterW
            ),
            topLeft = Offset(radioLeft, scaleTop),
            size = Size(halfCenterW, scaleH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Red needle
        val nX = radioLeft + halfCenterW * 0.55f
        drawLine(
            color = Color(0xFFFF3D00),
            start = Offset(nX, scaleTop),
            end = Offset(nX, scaleTop + scaleH),
            strokeWidth = 1.5.dp.toPx()
        )

        // Center Bottom: Piano Keys & VU Meter
        val botTop = tapeTop + tapeHeight + 3.dp.toPx()
        val botHeight = centerHeight - tapeHeight - 6.dp.toPx()

        // 6 Mini Keys on the left
        val kWidth = (halfCenterW - 5 * 1.5.dp.toPx()) / 6
        for (i in 0 until 6) {
            val kx = centerLeft + 2.dp.toPx() + i * (kWidth + 1.5.dp.toPx())
            drawRoundRect(
                color = if (i == 0) Color(0xFFC62828) else Color(0xFFFFFFFF),
                topLeft = Offset(kx, botTop),
                size = Size(kWidth, botHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }

        // VU meter on the right
        val vuLeft = centerLeft + halfCenterW + 2.dp.toPx()
        drawRoundRect(
            color = Color(0xFF101216),
            topLeft = Offset(vuLeft, botTop),
            size = Size(halfCenterW, botHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Green, yellow, red LEDs
        val barCount = 10
        val barW = (halfCenterW - 6.dp.toPx()) / barCount
        for (b in 0 until barCount) {
            val bx = vuLeft + 3.dp.toPx() + b * barW
            val barColor = when {
                b < 6 -> Color(0xFF00E676)
                b < 8 -> Color(0xFFFFD600)
                else -> Color(0xFFFF1744)
            }
            drawRect(
                color = barColor,
                topLeft = Offset(bx, botTop + 2.dp.toPx()),
                size = Size(barW * 0.7f, botHeight - 4.dp.toPx())
            )
        }
    }
}

/**
 * Preview Graphic for Transistor Radio (Querformat ohne Kassette)
 */
@Composable
fun TransistorRadioPreviewGraphic(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0E12))
            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0xFF2C323D), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val w = size.width
        val h = size.height

        val boxWidth = (w - 12.dp.toPx()).coerceAtMost(h * 2.1f)
        val boxLeft = (w - boxWidth) / 2
        val boxTop = 8.dp.toPx()
        val boxHeight = h - 14.dp.toPx()

        // Chrome Antenna on top
        drawLine(
            color = Color(0xFFD6DBE5),
            start = Offset(boxLeft + 16.dp.toPx(), boxTop - 3.dp.toPx()),
            end = Offset(boxLeft + boxWidth - 20.dp.toPx(), boxTop - 3.dp.toPx()),
            strokeWidth = 2.5.dp.toPx()
        )

        // Dark leatherette body with chrome trim
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B2D33), Color(0xFF1E2025), Color(0xFF141518))
            ),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFFFFB300) else Color(0xFFCFD5E2),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Left half: Perforated speaker grill
        val halfW = (boxWidth - 6.dp.toPx()) / 2f
        val grillLeft = boxLeft + 3.dp.toPx()
        val grillTop = boxTop + 3.dp.toPx()
        val grillH = boxHeight - 6.dp.toPx()

        drawRoundRect(
            color = Color(0xFF191B20),
            topLeft = Offset(grillLeft, grillTop),
            size = Size(halfW, grillH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Dots in grid
        val cols = 7
        val rows = 5
        val dx = halfW / (cols + 1)
        val dy = grillH / (rows + 1)
        for (r in 1..rows) {
            for (c in 1..cols) {
                drawCircle(
                    color = Color(0xFF0A0C0E),
                    radius = 2.dp.toPx(),
                    center = Offset(grillLeft + c * dx, grillTop + r * dy)
                )
            }
        }

        // Right half top: Amber backlit FM scale
        val rightLeft = boxLeft + halfW + 4.dp.toPx()
        val rightW = halfW - 2.dp.toPx()
        val scaleH = grillH * 0.44f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2E1906), Color(0xFF4A2A0A), Color(0xFF2E1906))
            ),
            topLeft = Offset(rightLeft, grillTop),
            size = Size(rightW, scaleH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Red needle on scale
        val needleX = rightLeft + rightW * 0.58f
        drawLine(
            color = Color(0xFFFF2A00),
            start = Offset(needleX, grillTop + 1.dp.toPx()),
            end = Offset(needleX, grillTop + scaleH - 1.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // Slider track groove below scale
        val grooveTop = grillTop + scaleH + 2.dp.toPx()
        val grooveH = 6.dp.toPx()
        drawRoundRect(
            color = Color(0xFF090A0D),
            topLeft = Offset(rightLeft, grooveTop),
            size = Size(rightW, grooveH),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
        // Silver thumb slider on groove
        drawRoundRect(
            color = Color(0xFFE2E7F0),
            topLeft = Offset(needleX - 3.dp.toPx(), grooveTop - 1.dp.toPx()),
            size = Size(6.dp.toPx(), grooveH + 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Right half bottom: Left switch + Right volume dial
        val bottomTop = grooveTop + grooveH + 3.dp.toPx()
        val bottomH = grillH - scaleH - grooveH - 5.dp.toPx()

        // 3-way switch (left)
        val switchW = rightW * 0.28f
        drawRoundRect(
            color = Color(0xFF0C0D10),
            topLeft = Offset(rightLeft, bottomTop),
            size = Size(switchW, bottomH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF373B45),
            topLeft = Offset(rightLeft + 1.dp.toPx(), bottomTop + 2.dp.toPx()),
            size = Size(switchW - 2.dp.toPx(), bottomH * 0.35f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Rotary Volume Knob (right)
        val knobCenterX = rightLeft + rightW * 0.65f
        val knobCenterY = bottomTop + bottomH / 2f
        val knobR = bottomH * 0.42f

        drawCircle(
            color = Color(0xFF333842),
            radius = knobR,
            center = Offset(knobCenterX, knobCenterY)
        )
        drawCircle(
            color = Color(0xFF14161A),
            radius = knobR - 1.5.dp.toPx(),
            center = Offset(knobCenterX, knobCenterY)
        )
        // Notch line
        drawLine(
            color = Color(0xFFF0F4FC),
            start = Offset(knobCenterX, knobCenterY),
            end = Offset(knobCenterX + (knobR - 2.dp.toPx()) * 0.7f, knobCenterY - (knobR - 2.dp.toPx()) * 0.7f),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

/**
 * Realistische Vorschaugrafik für das cremefarbene Retro-Transistorradio mit Stoffbespannung
 */
@Composable
fun CreamTransistorRadioPreviewGraphic(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0E12))
            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0xFF2C323D), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val w = size.width
        val h = size.height

        val boxWidth = (w - 12.dp.toPx()).coerceAtMost(h * 2.1f)
        val boxLeft = (w - boxWidth) / 2
        val boxTop = 8.dp.toPx()
        val boxHeight = h - 14.dp.toPx()

        // 1. Leather handle on top
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFC79A63), Color(0xFFA77B47), Color(0xFF865D2E))
            ),
            topLeft = Offset(boxLeft + 20.dp.toPx(), boxTop - 5.dp.toPx()),
            size = Size(boxWidth - 40.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )

        // 2. Cream rounded cabinet body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF7F2E7), Color(0xFFEBE2D0), Color(0xFFDDD2BE))
            ),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFFFFB300) else Color(0xFFDFD4C0),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 3. Top control section (~46% height)
        val topH = (boxHeight - 6.dp.toPx()) * 0.46f
        val innerLeft = boxLeft + 3.dp.toPx()
        val innerTop = boxTop + 3.dp.toPx()
        val innerW = boxWidth - 6.dp.toPx()

        // Top panel background
        drawRoundRect(
            color = Color(0xFFECE4D2),
            topLeft = Offset(innerLeft, innerTop),
            size = Size(innerW, topH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Left ON switch + amber LED
        val switchW = 12.dp.toPx()
        val switchH = topH * 0.75f
        val switchLeft = innerLeft + 4.dp.toPx()
        val switchTop = innerTop + (topH - switchH) / 2f
        drawRoundRect(
            color = Color(0xFF161513),
            topLeft = Offset(switchLeft, switchTop),
            size = Size(switchW, switchH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Amber LED
        drawCircle(
            color = Color(0xFFFFB300),
            radius = 1.8.dp.toPx(),
            center = Offset(switchLeft + switchW / 2f, switchTop + 3.dp.toPx())
        )
        // Switch button
        drawRoundRect(
            color = Color(0xFFCBD2DF),
            topLeft = Offset(switchLeft + 1.dp.toPx(), switchTop + 6.dp.toPx()),
            size = Size(switchW - 2.dp.toPx(), switchH * 0.45f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Center wide FM scale
        val scaleLeft = switchLeft + switchW + 6.dp.toPx()
        val knobW = 20.dp.toPx()
        val scaleW = innerW - switchW - knobW - 16.dp.toPx()
        val scaleGlassH = topH * 0.58f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2E1906), Color(0xFF4A2A0A), Color(0xFF2E1906))
            ),
            topLeft = Offset(scaleLeft, switchTop),
            size = Size(scaleW, scaleGlassH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Glowing red needle
        val needleX = scaleLeft + scaleW * 0.55f
        drawLine(
            color = Color(0xFFFF2A00),
            start = Offset(needleX, switchTop + 1.dp.toPx()),
            end = Offset(needleX, switchTop + scaleGlassH - 1.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // Slider track below scale
        val sliderTrackTop = switchTop + scaleGlassH + 2.dp.toPx()
        val sliderTrackH = switchH - scaleGlassH - 2.dp.toPx()
        drawRoundRect(
            color = Color(0xFFD7CDB7),
            topLeft = Offset(scaleLeft, sliderTrackTop),
            size = Size(scaleW, sliderTrackH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        // Chrome thumb with red center mark
        drawRoundRect(
            color = Color(0xFFE2E7F0),
            topLeft = Offset(needleX - 3.dp.toPx(), sliderTrackTop),
            size = Size(6.dp.toPx(), sliderTrackH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(needleX, sliderTrackTop),
            end = Offset(needleX, sliderTrackTop + sliderTrackH),
            strokeWidth = 1.dp.toPx()
        )

        // Right Volume Knob
        val knobCenterX = innerLeft + innerW - knobW / 2f - 4.dp.toPx()
        val knobCenterY = innerTop + topH / 2f
        val knobR = knobW * 0.42f
        drawCircle(
            color = Color(0xFFCBD2DF),
            radius = knobR,
            center = Offset(knobCenterX, knobCenterY)
        )
        drawCircle(
            color = Color(0xFFA5AFC0),
            radius = knobR * 0.85f,
            center = Offset(knobCenterX, knobCenterY)
        )

        // 4. Lower woven cloth speaker grille (~54% height across FULL width)
        val clothTop = innerTop + topH + 1.5.dp.toPx()
        val clothH = boxHeight - topH - 8.dp.toPx()

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFBA9E77), Color(0xFFA68B66), Color(0xFF967D5B))
            ),
            topLeft = Offset(innerLeft, clothTop),
            size = Size(innerW, clothH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Grid weaving threads
        val threadStep = 3.5.dp.toPx()
        val cCols = (innerW / threadStep).toInt()
        val cRows = (clothH / threadStep).toInt()
        for (cr in 0..cRows) {
            val cy = clothTop + cr * threadStep
            for (cc in 0..cCols) {
                val cx = innerLeft + cc * threadStep
                val isLight = (cr + cc) % 2 == 0
                if (isLight) {
                    drawCircle(
                        color = Color(0xFFE2D4BF).copy(alpha = 0.7f),
                        radius = 0.8.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

