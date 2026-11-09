package de.tedoki.mixtape.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.RecordingMode
import de.tedoki.mixtape.model.TrackMarker
import de.tedoki.mixtape.ui.components.CassetteView
import de.tedoki.mixtape.ui.theme.CassetteInkBlue
import de.tedoki.mixtape.ui.theme.CassetteLabelPaper
import de.tedoki.mixtape.ui.theme.CaveatFontFamily
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark
import de.tedoki.mixtape.ui.theme.ShareTechMonoFontFamily
import de.tedoki.mixtape.util.AudioFileUtils

@Composable
fun CassetteDetailScreen(
    cassette: Cassette,
    tracks: List<TrackMarker>,
    onPlayInDeck: (Cassette) -> Unit,
    onEditTape: (Cassette) -> Unit,
    onDeleteTape: (Cassette) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var refreshCounter by remember { mutableIntStateOf(0) }

    val audioFileInfo = remember(cassette, refreshCounter) {
        AudioFileUtils.inspectAudioFile(context, cassette)
    }

    val recordedMinutes = cassette.durationSec / 60
    val totalCapacityMin = 60 // C-60 format
    val fillRatio = (cassette.durationSec.toFloat() / 3600f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar: [<] Title, Edit, Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück zum Regal",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cassette.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "C-60 Audiokassette • Regal-Details",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            IconButton(onClick = { onEditTape(cassette) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Beschriftung bearbeiten",
                    tint = Color(0xFFCFD8DC)
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kassette löschen",
                    tint = Color(0xFFE57373)
                )
            }
        }

        // Full Cassette Graphic Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            CassetteView(
                cassette = cassette,
                isSpinning = false,
                tapePositionSec = cassette.currentPositionSec,
                totalCapacitySec = cassette.totalCapacitySec,
                modifier = Modifier.fillMaxWidth(),
                onLabelClick = { onEditTape(cassette) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary Action Button: "In Deck einlegen & Abspielen"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = { onPlayInDeck(cassette) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "In Deck einlegen & Abspielen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Specifications & Tape Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2127))
                .border(1.dp, Color(0xFF333842), RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "KASSETTEN-SPEZIFIKATION",
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300),
                letterSpacing = 1.sp
            )

            // Duration & C-60 capacity progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Format: C-60 (60 Minuten)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "$recordedMinutes / $totalCapacityMin Min belegt",
                        fontSize = 13.sp,
                        color = Color(0xFFFFB300),
                        fontFamily = ShareTechMonoFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { fillRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFB300),
                    trackColor = Color(0xFF2E3440)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C3038)))

            // Metadata fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Seite A", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text(cassette.sideA.ifBlank { "Seite A" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Seite B", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text(cassette.sideB.ifBlank { "Seite B" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (cassette.radioStationName.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Senderquelle: ${cassette.radioStationName}",
                        fontSize = 13.sp,
                        color = Color(0xFFCFD8DC)
                    )
                }
            }

            if (cassette.notes.isNotBlank()) {
                Text(
                    text = "Notizen: ${cassette.notes}",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio File & Storage Location Card (Technical file details & external playback)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2127))
                .border(1.dp, Color(0xFF333842), RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUDIODATEI & SPEICHERORT",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { refreshCounter++ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Datei-Info aktualisieren",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // File Name & Format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dateiname", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text(
                        text = audioFileInfo.fileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ShareTechMonoFontFamily,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Format / MIME", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text(
                        text = audioFileInfo.mimeType,
                        fontSize = 12.sp,
                        fontFamily = ShareTechMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            // Real File Size
            Column {
                Text("Dateigröße (Real ermittelt)", fontSize = 11.sp, color = Color(0xFF90A4AE))
                Text(
                    text = audioFileInfo.formattedSize,
                    fontSize = 13.sp,
                    fontFamily = ShareTechMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (audioFileInfo.sizeBytes > 0L) Color(0xFF81C784) else Color(0xFFE57373)
                )
                Text(
                    text = "Dezimal (MB) und Binär (MiB) direkt aus Dateisystem ermittelt.",
                    fontSize = 10.sp,
                    color = Color(0xFF78909C)
                )
            }

            // File Status
            Column {
                Text("Dateistatus", fontSize = 11.sp, color = Color(0xFF90A4AE))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                audioFileInfo.isValidAudioFile -> Color(0xFF1B3B2B)
                                audioFileInfo.exists && audioFileInfo.sizeBytes == 0L -> Color(0xFF3E2F1A)
                                else -> Color(0xFF381B1B)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when {
                            audioFileInfo.isValidAudioFile -> Icons.Default.CheckCircle
                            audioFileInfo.exists && audioFileInfo.sizeBytes == 0L -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when {
                            audioFileInfo.isValidAudioFile -> Color(0xFF4CAF50)
                            audioFileInfo.exists && audioFileInfo.sizeBytes == 0L -> Color(0xFFFFB300)
                            else -> Color(0xFFE57373)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = audioFileInfo.statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            audioFileInfo.isValidAudioFile -> Color(0xFFA5D6A7)
                            audioFileInfo.exists && audioFileInfo.sizeBytes == 0L -> Color(0xFFFFE082)
                            else -> Color(0xFFEF9A9A)
                        }
                    )
                }
            }

            // Storage Location (Absolute Filesystem Path)
            Column {
                Text("Speicherort (Dateisystempfad)", fontSize = 11.sp, color = Color(0xFF90A4AE))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF14161A))
                        .border(1.dp, Color(0xFF2C3038), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = audioFileInfo.absolutePath,
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5),
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                if (audioFileInfo.isInternalStorage) {
                    Text(
                        text = "Hinweis: Interner privater App-Speicher (filesDir). Aus Android-Sicherheitsgründen für normale Dateimanager ohne Root nicht direkt sichtbar.",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 13.sp
                    )
                }
            }

            // Action: "In externem Player öffnen"
            Button(
                onClick = {
                    if (!audioFileInfo.isValidAudioFile) {
                        Toast.makeText(
                            context,
                            "Keine abspielbare Audiodatei: ${audioFileInfo.statusText}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        when (val res = AudioFileUtils.openInExternalPlayer(context, cassette)) {
                            is AudioFileUtils.OpenResult.Success -> {
                                Toast.makeText(context, "Externer Audioplayer wird gestartet…", Toast.LENGTH_SHORT).show()
                            }
                            is AudioFileUtils.OpenResult.Error -> {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (audioFileInfo.isValidAudioFile) Color(0xFF263238) else Color(0xFF1E2127),
                    contentColor = if (audioFileInfo.isValidAudioFile) Color(0xFFFFB300) else Color(0xFF78909C)
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                    1.dp,
                    if (audioFileInfo.isValidAudioFile) Color(0xFFFFB300) else Color(0xFF37474F)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "In externem Player öffnen",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recorded Song List Card (J-Card Tracklist)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2127))
                .border(1.dp, Color(0xFF333842), RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TITELVERZEICHNIS (${if (tracks.isNotEmpty()) tracks.size else cassette.trackCount} TITEL)",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "METADATEN",
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFF78909C)
                )
            }

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF15171C))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "1. ${cassette.radioStationName.ifBlank { "Radio" }} - Durchgehender Mitschnitt ($recordedMinutes Min)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCFD8DC)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEachIndexed { index, track ->
                        val min = track.startOffsetSec / 60
                        val sec = track.startOffsetSec % 60
                        val timeStr = String.format("%02d:%02d", min, sec)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF15171C))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%02d.", index + 1),
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.width(28.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (track.artist.isNotBlank()) {
                                    Text(
                                        text = track.artist,
                                        fontSize = 12.sp,
                                        color = Color(0xFF90A4AE)
                                    )
                                }
                            }

                            Text(
                                text = timeStr,
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Kassette löschen?") },
            text = { Text("Möchtest du das Mixtape \"${cassette.title}\" wirklich aus dem Regal entfernen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTape(cassette)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen", color = Color.White)
                }
            },
            containerColor = Color(0xFF262A33),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCFD8DC)
        )
    }
}
