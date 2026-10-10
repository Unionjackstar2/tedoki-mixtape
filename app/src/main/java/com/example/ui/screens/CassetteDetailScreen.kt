package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Cassette
import com.example.model.RecordingMode
import com.example.model.TrackMarker
import com.example.ui.components.CassetteView
import com.example.ui.theme.CassetteInkBlue
import com.example.ui.theme.CassetteLabelPaper
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.DeckSurfaceDark
import com.example.ui.theme.ShareTechMonoFontFamily

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

    var showDeleteConfirm by remember { mutableStateOf(false) }

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
