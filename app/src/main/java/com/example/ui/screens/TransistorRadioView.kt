package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.ui.components.TransistorTuningScale
import com.example.ui.components.TransistorVolumeKnob

enum class TransistorBandPowerMode {
    FM,
    AM,
    OFF
}

/**
 * Fotorealistisches Vintage Transistorradio (Querformat / Landscape)
 * Exakte Umsetzung der Bildvorlage:
 * - Horizontales 16:9-Chassis mit feiner dunkler Ledertextur und Chrom-Zierrahmen
 * - Chrom-Teleskopantenne oben auf dem Gehäuse & Leder-Trageschlaufenbeschläge
 * - Links: Großes Lautsprechergitter mit kreisrunder Perforations-Matrix
 * - Rechts oben: Bernsteinfarben hinterleuchtete UKW-Skala mit horizontalem Riffelschieber
 * - Rechts unten: 3-Wege-Wahlschalter (FM / AM / OFF) & drehbarer Lautstärkeregler (bidirektional)
 */
@Composable
fun TransistorRadioView(
    currentStation: RadioStation?,
    tunedFrequency: Float,
    isCurrentStationFavorite: Boolean,
    isRadioPlaying: Boolean = true,
    onFrequencyChange: (Float) -> Unit,
    onPowerModeChange: (TransistorBandPowerMode) -> Unit = {},
    onToggleFavorite: () -> Unit,
    onOpenStations: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bandPowerMode by remember { mutableStateOf(TransistorBandPowerMode.FM) }
    val isPoweredOn = bandPowerMode != TransistorBandPowerMode.OFF

    fun updateBandPowerMode(newMode: TransistorBandPowerMode) {
        bandPowerMode = newMode
        onPowerModeChange(newMode)
    }

    // Hintergrund: Gemütliche warme Vintage-Holztisch-Atmosphäre
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF332014),
                        Color(0xFF22140B),
                        Color(0xFF140B06),
                        Color(0xFF0A0503)
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Diskrete Kopfzeile mit Icons für Sendersuche, Favoriten & Einstellungen
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onOpenStations,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Senderliste",
                        tint = Color(0xFFD6A265)
                    )
                }
                Text(
                    text = if (isPoweredOn) (currentStation?.name ?: "UKW TRANSISTOR") else "RADIO AUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPoweredOn) Color(0xFFFFB300) else Color(0xFF757575)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentStationFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorit",
                        tint = if (isCurrentStationFavorite) Color(0xFFFFB300) else Color(0xFFB0BEC5)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color(0xFFCFD8DC)
                    )
                }
            }
        }

        // Haupt-Radiobox mit authentischem horizontalem Seitenverhältnis (~ 1.85 : 1)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val radioAspectRatio = 1.88f

            Box(
                modifier = Modifier
                    .aspectRatio(radioAspectRatio, matchHeightConstraintsFirst = true)
                    .fillMaxSize()
            ) {
                // 1. Oben liegende Chrom-Teleskopantenne (Exakt wie im Foto)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(14.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2f
                        val rodStart = 20.dp.toPx()
                        val rodEnd = size.width - 24.dp.toPx()

                        // Hinterer Antennenschaft
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFEEEEEE), Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFFECEFF1))
                            ),
                            start = Offset(rodStart, midY),
                            end = Offset(rodEnd, midY),
                            strokeWidth = 5.dp.toPx()
                        )
                        // Segmentschritte (Verbindungsringe)
                        val step1 = rodStart + (rodEnd - rodStart) * 0.42f
                        val step2 = rodStart + (rodEnd - rodStart) * 0.76f
                        for (sx in listOf(step1, step2)) {
                            drawCircle(
                                color = Color(0xFFCFD8DC),
                                radius = 4.2.dp.toPx(),
                                center = Offset(sx, midY)
                            )
                        }
                        // Gerändelter Antennenkopf ganz links
                        drawRoundRect(
                            color = Color(0xFFECEFF1),
                            topLeft = Offset(rodStart - 10.dp.toPx(), midY - 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 10.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                        // Gelenksockel rechts
                        drawCircle(
                            color = Color(0xFF455A64),
                            radius = 6.dp.toPx(),
                            center = Offset(rodEnd + 6.dp.toPx(), midY)
                        )
                    }
                }

                // 2. Gehäusefüße (Rubber Feet unten)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(10.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(Color(0xFF0F1114))
                        )
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(Color(0xFF0F1114))
                        )
                    }
                }

                // 3. Haupt-Radiogehäuse mit Ledertextur & poliertem Chrom-Zierrahmen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF26282E),
                                    Color(0xFF1B1C20),
                                    Color(0xFF141518),
                                    Color(0xFF1F2025)
                                )
                            )
                        )
                        // Doppelter Zierrahmen: Äußerer Chromrand + tiefe Innenkante
                        .border(3.5.dp, Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFD4DAE6),
                                Color(0xFF9EA7B8),
                                Color(0xFFE2E7F2),
                                Color(0xFF6B7382),
                                Color(0xFFBDC5D4)
                            )
                        ), RoundedCornerShape(18.dp))
                        .padding(8.dp)
                ) {
                    // Frontblende zweigeteilt: Links Lautsprechergitter, Rechts Bedienfeld
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF16181C)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ==========================================
                        // LINKE HÄLFTE: Perforiertes Lochgitter
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E2026))
                                .border(1.dp, Color(0xFF2B303A), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            // Canvas-Rendering der Perforations-Matrix
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Subtiler horizontaler Verlauf des Gitterblechs
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF2B2E36), Color(0xFF212329), Color(0xFF1C1E23))
                                    ),
                                    size = size,
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )

                                val cols = 15
                                val rows = 11
                                val xStep = w / (cols + 1)
                                val yStep = h / (rows + 1)
                                val holeRadius = (xStep.coerceAtMost(yStep) * 0.36f)

                                for (r in 1..rows) {
                                    val y = r * yStep
                                    for (c in 1..cols) {
                                        val x = c * xStep

                                        // Akustikloch: Tiefer schwarzer Kern
                                        drawCircle(
                                            color = Color(0xFF090A0C),
                                            radius = holeRadius,
                                            center = Offset(x, y)
                                        )
                                        // Oberer Innenschatten
                                        drawCircle(
                                            color = Color(0xFF000000),
                                            radius = holeRadius * 0.85f,
                                            center = Offset(x, y - 0.5f)
                                        )
                                        // Untere Reflexionskante (gestanztes Metall)
                                        drawCircle(
                                            color = Color(0xFF454B57).copy(alpha = 0.45f),
                                            radius = holeRadius,
                                            center = Offset(x, y + 0.8f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // RECHTE HÄLFTE: Bedienelemente & FM-Skala
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Oben: Bernsteinfarben beleuchtete FM-Skala mit Schieberegler
                            TransistorTuningScale(
                                currentFrequencyMhz = tunedFrequency,
                                isPoweredOn = isPoweredOn,
                                onFrequencyChange = { newF ->
                                    if (isPoweredOn) {
                                        onFrequencyChange(newF)
                                    }
                                }
                            )

                            // Unten: Schiebeschalter (FM / AM / OFF) & Lautstärkerad
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Vertikaler 3-Wege Schiebeschalter (FM / AM / OFF)
                                Box(
                                    modifier = Modifier
                                        .weight(0.42f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Vertikale Führungsschiene mit Schiebeknopf
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(96.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0A0C0E))
                                                .border(1.dp, Color(0xFF282C36), RoundedCornerShape(4.dp))
                                                .padding(vertical = 4.dp),
                                            contentAlignment = when (bandPowerMode) {
                                                TransistorBandPowerMode.FM -> Alignment.TopCenter
                                                TransistorBandPowerMode.AM -> Alignment.Center
                                                TransistorBandPowerMode.OFF -> Alignment.BottomCenter
                                            }
                                        ) {
                                            // Gerillter schwarzer Wipp-/Schiebeschalter wie im Foto
                                            Box(
                                                modifier = Modifier
                                                    .width(28.dp)
                                                    .height(26.dp)
                                                    .shadow(2.dp, RoundedCornerShape(3.dp))
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color(0xFF383C45), Color(0xFF212328), Color(0xFF141519))
                                                        )
                                                    )
                                                    .border(1.dp, Color(0xFF4C5360), RoundedCornerShape(3.dp))
                                                    .clickable {
                                                        // Durchschalten: FM -> AM -> OFF -> FM
                                                        val next = when (bandPowerMode) {
                                                            TransistorBandPowerMode.FM -> TransistorBandPowerMode.AM
                                                            TransistorBandPowerMode.AM -> TransistorBandPowerMode.OFF
                                                            TransistorBandPowerMode.OFF -> TransistorBandPowerMode.FM
                                                        }
                                                        updateBandPowerMode(next)
                                                    }
                                            ) {
                                                // Riffeln auf dem Schalter
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val lines = 4
                                                    val step = size.height / (lines + 1)
                                                    for (i in 1..lines) {
                                                        val ly = i * step
                                                        drawLine(
                                                            color = Color(0xFF0F1014),
                                                            start = Offset(3.dp.toPx(), ly),
                                                            end = Offset(size.width - 3.dp.toPx(), ly),
                                                            strokeWidth = 1.2.dp.toPx()
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Textbeschriftungen neben dem Schalter: FM / AM / OFF
                                        Column(
                                            modifier = Modifier.height(96.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                text = "FM",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bandPowerMode == TransistorBandPowerMode.FM) Color(0xFFFFB300) else Color(0xFFCFD4DC),
                                                modifier = Modifier.clickable { updateBandPowerMode(TransistorBandPowerMode.FM) }
                                            )
                                            Text(
                                                text = "AM",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bandPowerMode == TransistorBandPowerMode.AM) Color(0xFFFFB300) else Color(0xFFCFD4DC),
                                                modifier = Modifier.clickable { updateBandPowerMode(TransistorBandPowerMode.AM) }
                                            )
                                            Text(
                                                text = "OFF",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bandPowerMode == TransistorBandPowerMode.OFF) Color(0xFFFF1744) else Color(0xFF8B92A0),
                                                modifier = Modifier.clickable { updateBandPowerMode(TransistorBandPowerMode.OFF) }
                                            )
                                        }
                                    }
                                }

                                // 2. Großer Dreh-Lautstärkeregler (Rotary Volume Knob)
                                Box(
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TransistorVolumeKnob(
                                        size = 135.dp
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
