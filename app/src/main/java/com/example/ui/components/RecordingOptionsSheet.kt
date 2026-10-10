package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingOptionsSheet(
    currentMode: RecordingMode,
    onModeSelect: (RecordingMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E2127),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: "Aufnahmeoptionen" and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aufnahmeoptionen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 1: Voller Mitschnitt (empfohlen)
            val isContinuous = currentMode == RecordingMode.CONTINUOUS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isContinuous) Color(0xFF262A33) else Color(0xFF16181D))
                    .border(
                        width = 1.dp,
                        color = if (isContinuous) Color(0xFFFFB300) else Color(0xFF37474F),
                        shape = RoundedCornerShape(8.dp)
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
                            text = "Komplette Aufnahme als eine Audiodatei mit Metadaten der erkannten Songs. Wie bei einer echten Kassette.",
                            fontSize = 13.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Continuous waveform graphic with markers
                WaveformGraphic(isSegmented = false)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Automatisch in einzelne Clips schneiden
            val isClips = currentMode == RecordingMode.CLIPS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isClips) Color(0xFF262A33) else Color(0xFF16181D))
                    .border(
                        width = 1.dp,
                        color = if (isClips) Color(0xFFFFB300) else Color(0xFF37474F),
                        shape = RoundedCornerShape(8.dp)
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
                            text = "Songs werden automatisch erkannt und als einzelne Audiodateien gespeichert.",
                            fontSize = 13.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented waveform graphic
                WaveformGraphic(isSegmented = true)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WaveformGraphic(isSegmented: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F1115))
            .padding(horizontal = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Waveform bars
        val barCount = 48
        val barWidth = width / barCount
        val heights = listOf(
            0.3f, 0.5f, 0.8f, 0.6f, 0.9f, 0.4f, 0.7f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f,
            0.7f, 0.4f, 0.9f, 0.7f, 0.5f, 0.8f, 0.3f, 0.6f, 0.8f, 0.4f, 0.9f, 0.5f,
            0.3f, 0.7f, 0.5f, 0.8f, 0.6f, 0.4f, 0.9f, 0.3f, 0.8f, 0.5f, 0.6f, 0.7f,
            0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.6f, 0.7f, 0.4f, 0.8f, 0.5f, 0.3f, 0.2f
        )

        for (i in 0 until barCount) {
            val barH = (heights[i % heights.size] * (height * 0.8f))
            val x = i * barWidth + barWidth / 2
            val isCut = isSegmented && (i == 16 || i == 32)

            if (isCut) {
                // Gap / divider for segmented clips
                drawLine(
                    color = Color(0xFFFFB300),
                    start = Offset(x, 2.dp.toPx()),
                    end = Offset(x, height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            } else {
                drawLine(
                    color = Color(0xFFFFB300).copy(alpha = 0.85f),
                    start = Offset(x, centerY - barH / 2),
                    end = Offset(x, centerY + barH / 2),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
