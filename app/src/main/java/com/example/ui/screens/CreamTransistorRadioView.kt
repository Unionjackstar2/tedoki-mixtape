package com.example.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.util.HapticUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Fotorealistisches Vintage Transistorradio in warmem Cremeweiß (Landscape)
 * Exakt nach der Bildvorlage:
 * - Oben: Lederner Trageriemen mit hellbrauner Ziernaht und Chrom-Halterungen,
 *         dahinter liegende verchromte Teleskopantenne
 * - Cremefarbenes, abgerundetes Gehäuse mit sanften Kantenfasen und Gummifüßen
 * - Obere Leiste:
 *   - Links: Vertikaler Ein-/Ausschiebeschalter mit bernsteinfarbener LED-Kontrollleuchte (ON / OFF)
 *   - Mitte: Breites beleuchtetes Bernsteinglas-Skalenmodul (FM 88..108 MHz, leuchtend rote Nadel)
 *            mit direkt darunter liegendem geriffeltem Chrom-Schieberegler (roter Zentralstrich & Haptik)
 *   - Rechts: Konzentrisch gebürsteter Aluminium-Lautstärkeregler (bidirektional mit Hardwartasten synchronisiert)
 * - Untere Hälfte (über die gesamte Breite): Detailverliebte Stoffbespannung (gewebter Akustikstoff / Tweed)
 */
@Composable
fun CreamTransistorRadioView(
    currentStation: RadioStation?,
    tunedFrequency: Float,
    isCurrentStationFavorite: Boolean,
    isRadioPlaying: Boolean = true,
    onFrequencyChange: (Float) -> Unit,
    onPowerSwitch: (Boolean) -> Unit = {},
    onToggleFavorite: () -> Unit,
    onOpenStations: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPoweredOn by remember { mutableStateOf(true) }

    fun setPower(powered: Boolean) {
        isPoweredOn = powered
        onPowerSwitch(powered)
    }

    // Warmer Vintage-Wohnzimmer-Hintergrund
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF382618),
                        Color(0xFF26190E),
                        Color(0xFF160E08),
                        Color(0xFF0D0805)
                    ),
                    radius = 1300f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Diskrete Kopfzeile für Navigation (Senderliste, Favorit, Einstellungen)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
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
                        tint = Color(0xFFE2C498)
                    )
                }
                Text(
                    text = if (isPoweredOn) (currentStation?.name ?: "UKW RETRO TRANSISTOR") else "RADIO AUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPoweredOn) Color(0xFFFFB300) else Color(0xFF8D7F73)
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
                        tint = if (isCurrentStationFavorite) Color(0xFFFFB300) else Color(0xFFC9BDB0)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color(0xFFDFD7CC)
                    )
                }
            }
        }

        // Haupt-Radiobox mit Querformat (~ 1.82 : 1)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val radioAspectRatio = 1.82f

            Box(
                modifier = Modifier
                    .aspectRatio(radioAspectRatio, matchHeightConstraintsFirst = true)
                    .fillMaxSize()
            ) {
                // 1. Oben aufliegender Leder-Tragegriff & Teleskopantenne
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(24.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height * 0.7f
                        val rodStart = 16.dp.toPx()
                        val rodEnd = size.width - 24.dp.toPx()

                        // A. Dahinterliegende Teleskopantenne
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFFECEFF1))
                            ),
                            start = Offset(rodStart, midY - 6.dp.toPx()),
                            end = Offset(rodEnd, midY - 6.dp.toPx()),
                            strokeWidth = 4.5.dp.toPx()
                        )
                        // Kugelköpfiger Antennenabschluss ganz links
                        drawCircle(
                            color = Color(0xFFECEFF1),
                            radius = 4.dp.toPx(),
                            center = Offset(rodStart - 4.dp.toPx(), midY - 6.dp.toPx())
                        )

                        // B. Leder-Tragegriff (Hellbraun mit Ziernähten)
                        val handleStart = 28.dp.toPx()
                        val handleEnd = size.width - 28.dp.toPx()
                        val handleRectTop = midY - 3.dp.toPx()
                        val handleHeight = 10.dp.toPx()

                        // Lederriemen-Körper
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFC79A63), Color(0xFFA77B47), Color(0xFF865D2E))
                            ),
                            topLeft = Offset(handleStart, handleRectTop),
                            size = Size(handleEnd - handleStart, handleHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Feine Ziernaht oben und unten auf dem Leder
                        val stitchPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#EED8BE")
                            strokeWidth = 1.2.dp.toPx()
                            style = android.graphics.Paint.Style.STROKE
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 5f), 0f)
                        }
                        drawContext.canvas.nativeCanvas.drawLine(
                            handleStart + 6.dp.toPx(), handleRectTop + 2.dp.toPx(),
                            handleEnd - 6.dp.toPx(), handleRectTop + 2.dp.toPx(),
                            stitchPaint
                        )
                        drawContext.canvas.nativeCanvas.drawLine(
                            handleStart + 6.dp.toPx(), handleRectTop + handleHeight - 2.dp.toPx(),
                            handleEnd - 6.dp.toPx(), handleRectTop + handleHeight - 2.dp.toPx(),
                            stitchPaint
                        )

                        // Chrom-Halterungen links und rechts
                        for (hx in listOf(handleStart - 2.dp.toPx(), handleEnd - 10.dp.toPx())) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFBDC5D4), Color(0xFF788395), Color(0xFFE2E7F2))
                                ),
                                topLeft = Offset(hx, handleRectTop - 4.dp.toPx()),
                                size = Size(12.dp.toPx(), handleHeight + 7.dp.toPx()),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                }

                // 2. Gehäusefüße (Rubber Feet unten)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(10.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                                .background(Color(0xFF121417))
                        )
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                                .background(Color(0xFF121417))
                        )
                    }
                }

                // 3. Haupt-Radiogehäuse (Cremeweiß / Ivory mit weichen Rundungen & Randfase)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFAF6EE),
                                    Color(0xFFF1EAD9),
                                    Color(0xFFE5DCC9),
                                    Color(0xFFDED4C0)
                                )
                            )
                        )
                        // Sanfte äußere Kantenfase
                        .border(
                            3.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFEFE8D8),
                                    Color(0xFFD5CAAe),
                                    Color(0xFFF4EFE3)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(10.dp)
                ) {
                    // Frontseite zweigeteilt:
                    // Oben: Bedienfeld mit Schalter, Skala, Slider & Volume (~46% Höhe)
                    // Unten: Durchgehende Stoffbespannung über die volle Breite (~54% Höhe)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFECE4D2)),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // ==========================================
                        // OBERER TEIL: Bedienfeld
                        // ==========================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.48f)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. LINKS: ON/OFF Schiebeschalter mit LED
                            CreamPowerSwitch(
                                isPoweredOn = isPoweredOn,
                                onToggle = { setPower(!isPoweredOn) },
                                modifier = Modifier
                                    .width(46.dp)
                                    .fillMaxHeight()
                            )

                            // 2. MITTE: Breites FM-Tuning-Modul (Skala & Slider)
                            CreamFmTuningModule(
                                currentFrequencyMhz = tunedFrequency,
                                isPoweredOn = isPoweredOn,
                                onFrequencyChange = { newF ->
                                    if (isPoweredOn) {
                                        onFrequencyChange(newF)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            // 3. RECHTS: Dreh-Lautstärkeregler (Aluminium mit Punktskala)
                            CreamVolumeKnob(
                                isPoweredOn = isPoweredOn,
                                modifier = Modifier
                                    .width(84.dp)
                                    .fillMaxHeight()
                            )
                        }

                        // Trennfuge zwischen Bedienblende und Stoffbespannung
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF7A6A55).copy(alpha = 0.5f),
                                            Color(0xFFFFFFFF).copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // ==========================================
                        // UNTERER TEIL: Stoffbespannung über die gesamte Breite
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.52f)
                                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        ) {
                            WovenFabricSpeakerGrille(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vertikaler Ein-/Ausschalter mit bernsteinfarbener Kontroll-LED exakt wie im Foto
 */
@Composable
fun CreamPowerSwitch(
    isPoweredOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // "ON" Beschriftung
        Text(
            text = "ON",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPoweredOn) Color(0xFF3E3120) else Color(0xFF867664)
        )

        // Schalterschacht mit LED und Wipp-/Schiebetaste
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(68.dp)
                .shadow(2.dp, RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF161513))
                .border(1.dp, Color(0xFF736757), RoundedCornerShape(5.dp))
                .clickable { onToggle() }
                .padding(vertical = 4.dp, horizontal = 3.dp),
            contentAlignment = if (isPoweredOn) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Kleine bernsteinfarbene LED-Leuchte
                Canvas(modifier = Modifier.size(8.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.width / 2.3f

                    if (isPoweredOn) {
                        // Weicher warmer Schein um die LED
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF8F00).copy(alpha = 0.5f), Color.Transparent),
                                center = c,
                                radius = r * 2.2f
                            ),
                            radius = r * 2.2f,
                            center = c
                        )
                        // Glühender Kern
                        drawCircle(color = Color(0xFFFFD54F), radius = r, center = c)
                        drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.8f), radius = r * 0.45f, center = c)
                    } else {
                        // Ausgeschaltete matte Linse
                        drawCircle(color = Color(0xFF382D1D), radius = r, center = c)
                    }
                }

                // Gerillter silberner zylindrischer Schiebeknopf
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .shadow(3.dp, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE2E7F0),
                                    Color(0xFFBDC5D4),
                                    Color(0xFF8F98A8),
                                    Color(0xFFCAD2E0)
                                )
                            )
                        )
                        .border(0.8.dp, Color(0xFF6B7482), RoundedCornerShape(3.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ridges = 5
                        val step = size.height / (ridges + 1)
                        for (i in 1..ridges) {
                            val ly = i * step
                            drawLine(
                                color = Color(0xFF454B55),
                                start = Offset(2.dp.toPx(), ly),
                                end = Offset(size.width - 2.dp.toPx(), ly),
                                strokeWidth = 1.2.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.7f),
                                start = Offset(2.dp.toPx(), ly + 1f),
                                end = Offset(size.width - 2.dp.toPx(), ly + 1f),
                                strokeWidth = 0.7.dp.toPx()
                            )
                        }
                    }
                }
            }
        }

        // "OFF" Beschriftung
        Text(
            text = "OFF",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (!isPoweredOn) Color(0xFF3E3120) else Color(0xFF867664)
        )
    }
}

/**
 * Breites FM-Tuning-Modul:
 * Oben bernsteinfarbenes Glasfenster mit FM 88..108 MHz Skala & roter Leuchtnadel
 * Unten horizontale Schiebernut mit geriffeltem Chrom-Daumenschieber & rotem Mittelstrich
 */
@Composable
fun CreamFmTuningModule(
    currentFrequencyMhz: Float,
    isPoweredOn: Boolean,
    onFrequencyChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minFreq = 88.0f
    val maxFreq = 108.0f
    val clampedFreq = currentFrequencyMhz.coerceIn(minFreq, maxFreq)

    val context = LocalContext.current
    val view = LocalView.current
    var lastDetentIndex by remember { mutableIntStateOf((currentFrequencyMhz * 10f).roundToInt()) }

    fun updateFrequencyWithHaptics(newFreq: Float) {
        val clamped = newFreq.coerceIn(minFreq, maxFreq)
        val detent = (clamped * 10f).roundToInt()
        if (detent != lastDetentIndex) {
            lastDetentIndex = detent
            HapticUtils.performSliderTick(context, view)
        }
        onFrequencyChange(clamped)
    }

    val density = LocalDensity.current
    val leftMargin = with(density) { 36.dp.toPx() }
    val rightMargin = with(density) { 36.dp.toPx() }
    val sliderWidth = 32.dp
    val sliderHeight = 24.dp
    val sliderWidthPx = with(density) { sliderWidth.toPx() }

    // Äußeres vertieftes Modulgehäuse mit feinem Chrom/Schattenrand
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE2D8C3))
            .border(1.2.dp, Color(0xFFB5A68E), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Bernsteinfarben beleuchtetes Skalenfenster
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.66f)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isPoweredOn) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF160E04),
                                Color(0xFF2B1806),
                                Color(0xFF3E2007),
                                Color(0xFF1A0F04)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F0E0C), Color(0xFF171513))
                        )
                    }
                )
                .border(1.dp, Color(0xFF3D2C1B), RoundedCornerShape(4.dp))
                .pointerInput(isPoweredOn) {
                    if (!isPoweredOn) return@pointerInput
                    detectTapGestures { tapOffset ->
                        val usableW = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                        val fraction = ((tapOffset.x - leftMargin) / usableW).coerceIn(0f, 1f)
                        val newFreq = minFreq + fraction * (maxFreq - minFreq)
                        updateFrequencyWithHaptics(newFreq)
                    }
                }
                .pointerInput(isPoweredOn) {
                    if (!isPoweredOn) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val usableW = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                            val fraction = ((offset.x - leftMargin) / usableW).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val usableW = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                            val fraction = ((change.position.x - leftMargin) / usableW).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        }
                    )
                }
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val usableW = (widthPx - leftMargin - rightMargin).coerceAtLeast(1f)

            val freqFraction = ((clampedFreq - minFreq) / (maxFreq - minFreq)).coerceIn(0f, 1f)
            val needleX = leftMargin + freqFraction * usableW

            Canvas(modifier = Modifier.matchParentSize()) {
                val textColor = if (isPoweredOn) android.graphics.Color.parseColor("#FFA500") else android.graphics.Color.parseColor("#5A4528")
                val tickColor = if (isPoweredOn) Color(0xFFFFB300) else Color(0xFF5A4528)

                // "FM" Label links
                val fmPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 15.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText("FM", 8.dp.toPx(), 22.dp.toPx(), fmPaint)

                // "MHz" Label rechts
                val mhzPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 11.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawContext.canvas.nativeCanvas.drawText("MHz", widthPx - 8.dp.toPx(), 22.dp.toPx(), mhzPaint)

                // Frequenzen: 88, 90, 92, 94, 96, 98, 100, 102, 104, 106, 108
                val freqNumbers = listOf(88, 90, 92, 94, 96, 98, 100, 102, 104, 106, 108)
                val numberPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 11.5.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (num in freqNumbers) {
                    val frac = (num.toFloat() - minFreq) / (maxFreq - minFreq)
                    val x = leftMargin + frac * usableW
                    drawContext.canvas.nativeCanvas.drawText(num.toString(), x, 22.dp.toPx(), numberPaint)
                }

                // Horizontale Basislinie
                val baseLineY = 28.dp.toPx()
                drawLine(
                    color = tickColor.copy(alpha = if (isPoweredOn) 0.8f else 0.4f),
                    start = Offset(leftMargin, baseLineY),
                    end = Offset(widthPx - rightMargin, baseLineY),
                    strokeWidth = 1.dp.toPx()
                )

                // Teilstriche (Ticks) alle 0.5 MHz
                val totalSteps = ((maxFreq - minFreq) / 0.5f).toInt()
                for (step in 0..totalSteps) {
                    val f = minFreq + step * 0.5f
                    val frac = (f - minFreq) / (maxFreq - minFreq)
                    val x = leftMargin + frac * usableW

                    val isMajor = f in listOf(88f, 90f, 92f, 94f, 96f, 98f, 100f, 102f, 104f, 106f, 108f)
                    val isHalf = (f % 1.0f == 0f) && !isMajor

                    val tickHeight = when {
                        isMajor -> 8.5.dp.toPx()
                        isHalf -> 5.5.dp.toPx()
                        else -> 3.5.dp.toPx()
                    }

                    drawLine(
                        color = tickColor.copy(alpha = if (isMajor) 0.95f else if (isHalf) 0.75f else 0.45f),
                        start = Offset(x, baseLineY),
                        end = Offset(x, baseLineY + tickHeight),
                        strokeWidth = if (isMajor) 1.2.dp.toPx() else 0.8.dp.toPx()
                    )
                }

                // Glühende rote Skalennadel
                if (isPoweredOn) {
                    // Weicher roter Schein
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFF3D00).copy(alpha = 0.5f),
                                Color(0xFFFF1744).copy(alpha = 0.85f),
                                Color(0xFFFF3D00).copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            startX = needleX - 5.dp.toPx(),
                            endX = needleX + 5.dp.toPx()
                        ),
                        topLeft = Offset(needleX - 5.dp.toPx(), 2.dp.toPx()),
                        size = Size(10.dp.toPx(), heightPx - 4.dp.toPx())
                    )
                    // Scharfer roter Nadelstrich
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = Offset(needleX, 2.dp.toPx()),
                        end = Offset(needleX, heightPx - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                } else {
                    drawLine(
                        color = Color(0xFF6B7280),
                        start = Offset(needleX, 4.dp.toPx()),
                        end = Offset(needleX, heightPx - 4.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 2. Horizontale Schiebernut mit geriffeltem Chrom-Daumenschieber
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFD7CDB7))
                .border(1.dp, Color(0xFFB0A28A), RoundedCornerShape(3.dp))
                .pointerInput(isPoweredOn) {
                    if (!isPoweredOn) return@pointerInput
                    detectTapGestures { tapOffset ->
                        val trackW = (size.width - sliderWidthPx).coerceAtLeast(1f)
                        val fraction = ((tapOffset.x - sliderWidthPx / 2f) / trackW).coerceIn(0f, 1f)
                        val newFreq = minFreq + fraction * (maxFreq - minFreq)
                        updateFrequencyWithHaptics(newFreq)
                    }
                }
                .pointerInput(isPoweredOn) {
                    if (!isPoweredOn) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val trackW = (size.width - sliderWidthPx).coerceAtLeast(1f)
                            val fraction = ((offset.x - sliderWidthPx / 2f) / trackW).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val trackW = (size.width - sliderWidthPx).coerceAtLeast(1f)
                            val fraction = ((change.position.x - sliderWidthPx / 2f) / trackW).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        }
                    )
                }
        ) {
            val trackWidthPx = constraints.maxWidth.toFloat()
            val effectiveTrackW = (trackWidthPx - sliderWidthPx).coerceAtLeast(1f)

            val freqFrac = ((clampedFreq - minFreq) / (maxFreq - minFreq)).coerceIn(0f, 1f)
            val sliderLeftPx = freqFrac * effectiveTrackW

            // Vertiefungsspur & feine Teilstriche in der Nut
            Canvas(modifier = Modifier.matchParentSize()) {
                val midY = size.height / 2f
                drawLine(
                    color = Color(0xFF221F1A),
                    start = Offset(6.dp.toPx(), midY),
                    end = Offset(size.width - 6.dp.toPx(), midY),
                    strokeWidth = 2.2.dp.toPx()
                )

                // Feine Kalibrierticks in der Nut
                val tickCount = 20
                val tickStep = (size.width - 24.dp.toPx()) / tickCount
                for (i in 0..tickCount) {
                    val tx = 12.dp.toPx() + i * tickStep
                    drawLine(
                        color = Color(0xFF6B5F4E).copy(alpha = 0.6f),
                        start = Offset(tx, midY - 3.dp.toPx()),
                        end = Offset(tx, midY + 3.dp.toPx()),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }
            }

            // Geriffelter Chrom-Schieberknopf mit rotem Zentralstrich
            Box(
                modifier = Modifier
                    .offset { IntOffset(sliderLeftPx.roundToInt(), (constraints.maxHeight / 2 - sliderHeight.toPx() / 2).roundToInt()) }
                    .size(width = sliderWidth, height = sliderHeight)
                    .shadow(3.dp, RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFD2D9E5),
                                Color(0xFF9EA7B8),
                                Color(0xFFDDE3EE)
                            )
                        )
                    )
                    .border(0.8.dp, Color(0xFF6B7482), RoundedCornerShape(3.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    // Vertikale Riffeln links und rechts
                    val sideCount = 3
                    val step = (w * 0.36f) / (sideCount + 1)
                    for (i in 1..sideCount) {
                        // Links
                        val lx = i * step
                        drawLine(Color(0xFF4A5260), Offset(lx, 2.dp.toPx()), Offset(lx, h - 2.dp.toPx()), 1.dp.toPx())
                        // Rechts
                        val rx = w - (i * step)
                        drawLine(Color(0xFF4A5260), Offset(rx, 2.dp.toPx()), Offset(rx, h - 2.dp.toPx()), 1.dp.toPx())
                    }

                    // Roter Mittelstrich wie im Foto
                    val midX = w / 2f
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = Offset(midX, 2.dp.toPx()),
                        end = Offset(midX, h - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

/**
 * Konzentrisch gebürsteter Aluminium-Lautstärkeregler für das cremefarbene Radio
 */
@Composable
fun CreamVolumeKnob(
    isPoweredOn: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 82.dp
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolumeRatio by remember {
        mutableFloatStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume).coerceIn(0f, 1f)
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
                        val currentStreamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolumeRatio = (currentStreamVol.toFloat() / maxVolume).coerceIn(0f, 1f)
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(maxVolume) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                        val currentTouch = change.position
                        val prevTouch = currentTouch - dragAmount

                        val angleCurrent = atan2(currentTouch.y - center.y, currentTouch.x - center.x)
                        val anglePrev = atan2(prevTouch.y - center.y, prevTouch.x - center.x)

                        var angleDelta = angleCurrent - anglePrev
                        if (angleDelta > PI) angleDelta -= (2 * PI).toFloat()
                        if (angleDelta < -PI) angleDelta += (2 * PI).toFloat()

                        val deltaRatio = angleDelta / (270f * PI.toFloat() / 180f)
                        val updatedRatio = (currentVolumeRatio + deltaRatio).coerceIn(0f, 1f)
                        currentVolumeRatio = updatedRatio

                        val newIndex = (updatedRatio * maxVolume).toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newIndex, 0)
                    }
                }
        ) {
            val cX = this.size.width / 2f
            val cY = this.size.height / 2f + 2.dp.toPx()
            val knobRadius = this.size.width * 0.31f
            val scaleRadius = this.size.width * 0.40f

            // "VOLUME" Beschriftung
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 9.sp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                color = android.graphics.Color.parseColor("#3E3224")
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.04f
            }
            drawContext.canvas.nativeCanvas.drawText("VOLUME", cX, cY - scaleRadius - 4f, textPaint)

            // Dotted Arc Skala (11 Punkte)
            val dotCount = 11
            for (i in 0 until dotCount) {
                val fraction = i.toFloat() / (dotCount - 1)
                val angleDeg = 135f + fraction * 270f
                val rad = Math.toRadians(angleDeg.toDouble())
                val dotX = cX + scaleRadius * cos(rad).toFloat()
                val dotY = cY + scaleRadius * sin(rad).toFloat()

                val isCurrentOrBelow = fraction <= (currentVolumeRatio + 0.04f)
                val dotColor = if (isCurrentOrBelow) Color(0xFF3E3120) else Color(0xFF9E9182)
                val dotRadius = if (i == 0 || i == dotCount - 1) 2.6f else 2.0f

                drawCircle(color = dotColor, radius = dotRadius, center = Offset(dotX, dotY))
            }

            // MIN & MAX
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 7.5.sp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                color = android.graphics.Color.parseColor("#5A4B3A")
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val minAngleRad = Math.toRadians(135.0)
            val minX = cX + (scaleRadius + 10f) * cos(minAngleRad).toFloat()
            val minY = cY + (scaleRadius + 12f) * sin(minAngleRad).toFloat() + 6f
            drawContext.canvas.nativeCanvas.drawText("MIN", minX, minY, labelPaint)

            val maxAngleRad = Math.toRadians(405.0)
            val maxX = cX + (scaleRadius + 10f) * cos(maxAngleRad).toFloat()
            val maxY = cY + (scaleRadius + 12f) * sin(maxAngleRad).toFloat() + 6f
            drawContext.canvas.nativeCanvas.drawText("MAX", maxX, maxY, labelPaint)

            // Äußerer Schatten
            drawCircle(color = Color(0x33000000), radius = knobRadius + 2.5f, center = Offset(cX, cY))

            // Äußerer gerändelter Metallring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFE2E7F0), Color(0xFFBDC5D4), Color(0xFF8F98A8),
                        Color(0xFFCAD2E0), Color(0xFFE2E7F0)
                    ),
                    center = Offset(cX, cY)
                ),
                radius = knobRadius,
                center = Offset(cX, cY)
            )

            // Konzentrisch gebürsteter Kern
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCBD2DF), Color(0xFFA5AFC0), Color(0xFF7E8A9E)),
                    center = Offset(cX, cY),
                    radius = knobRadius - 2f
                ),
                radius = knobRadius - 2f,
                center = Offset(cX, cY)
            )

            // Radialer Glanzkegel
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.35f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.25f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f)
                    ),
                    center = Offset(cX, cY)
                ),
                radius = knobRadius - 3f,
                center = Offset(cX, cY)
            )

            // Weißer Indikatorstrich
            val needleRad = Math.toRadians((135f + currentVolumeRatio * 270f).toDouble())
            val startDist = knobRadius * 0.35f
            val endDist = knobRadius * 0.88f
            val startX = cX + startDist * cos(needleRad).toFloat()
            val startY = cY + startDist * sin(needleRad).toFloat()
            val endX = cX + endDist * cos(needleRad).toFloat()
            val endY = cY + endDist * sin(needleRad).toFloat()

            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(startX + 0.8f, startY + 0.8f),
                end = Offset(endX + 0.8f, endY + 0.8f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.0f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Detailverliebte Stoffbespannung (gewebter Akustikstoff / Vintage Tweed Grill Cloth)
 * über die gesamte Breite des Lautsprechers exakt wie im Foto:
 * - Warmer Haferflocken- / Weizenton mit horizontalen & vertikalen Schuss- und Kettfäden
 * - Dreidimensionale Fadenknoten mit Lichtreflexionen und Mikroschatten
 */
@Composable
fun WovenFabricSpeakerGrille(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Warmer Basiston des Lautsprecherstoffs
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFBA9E77),
                    Color(0xFFA68B66),
                    Color(0xFF9E835E),
                    Color(0xFF8F7450)
                )
            ),
            size = size
        )

        // 2. Weben der Fadenmatrix (Tweed / Basketweave)
        // Jeder Rapportschnitt besteht aus überlappenden Fadenbändern
        val threadSpacing = 5.2.dp.toPx()
        val cols = (w / threadSpacing).toInt() + 1
        val rows = (h / threadSpacing).toInt() + 1

        val lightYarn = Color(0xFFE5D5BC)
        val midYarn = Color(0xFFC7B191)
        val shadowYarn = Color(0xFF5E4931)

        for (r in 0 until rows) {
            val y = r * threadSpacing
            for (c in 0 until cols) {
                val x = c * threadSpacing

                // Schachbrettartiges Wechseln von Kette und Schuss (Weave-Intersection)
                val isHorizontalDominant = (r + c) % 2 == 0

                if (isHorizontalDominant) {
                    // Horizontaler Fadenabschnitt
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(lightYarn, midYarn, shadowYarn),
                            startY = y,
                            endY = y + threadSpacing * 0.85f
                        ),
                        topLeft = Offset(x, y + threadSpacing * 0.1f),
                        size = Size(threadSpacing * 0.95f, threadSpacing * 0.8f),
                        cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
                    )
                } else {
                    // Vertikaler Fadenabschnitt
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(shadowYarn, midYarn, lightYarn),
                            startX = x,
                            endX = x + threadSpacing * 0.85f
                        ),
                        topLeft = Offset(x + threadSpacing * 0.1f, y),
                        size = Size(threadSpacing * 0.8f, threadSpacing * 0.95f),
                        cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
                    )
                }
            }
        }

        // 3. Subtiler Akustik-Vignettenschatten an den Rändern für plastische Gehäusetiefe
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x35000000)),
                center = Offset(w / 2f, h / 2f),
                radius = (w / 1.6f).coerceAtLeast(1f)
            ),
            size = size
        )

        // Oberer tiefer Schattenwurf der Frontblende auf den Stoff
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x66000000), Color.Transparent),
                startY = 0f,
                endY = 12.dp.toPx()
            ),
            size = size
        )
    }
}
