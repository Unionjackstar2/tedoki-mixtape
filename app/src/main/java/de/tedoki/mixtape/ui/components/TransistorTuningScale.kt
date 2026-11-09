package de.tedoki.mixtape.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.tedoki.mixtape.util.HapticUtils
import kotlin.math.roundToInt

/**
 * Fotorealistische Vintage UKW-Abstimmskala (Transistor Radio Tuning Scale)
 * Exakt nach der Bildvorlage:
 * - Oben: Warm bernsteinfarben hinterleuchtete Frequenzskala mit "FM", 88..108 "MHz",
 *   feinen Teilstrichen und glühender roter Nadel.
 * - Unten: Horizontale Führungsnut mit geriffeltem Chrom/Silber-Daumenschieber,
 *   der sich flüssig verschieben lässt und die UKW-Frequenz einstellt.
 */
@Composable
fun TransistorTuningScale(
    currentFrequencyMhz: Float,
    isPoweredOn: Boolean = true,
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
    val leftMargin = with(density) { 38.dp.toPx() }
    val rightMargin = with(density) { 38.dp.toPx() }
    val sliderWidth = 32.dp
    val sliderHeight = 26.dp
    val sliderWidthPx = with(density) { sliderWidth.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111317))
            .border(1.5.dp, Color(0xFF2C323D), RoundedCornerShape(6.dp))
            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        // 1. Skalenfenster (Bernsteinfarbenes Glas)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isPoweredOn) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF140C03),
                                Color(0xFF281604),
                                Color(0xFF381D05),
                                Color(0xFF180E03)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D0E11), Color(0xFF13151A))
                        )
                    }
                )
                .border(1.dp, Color(0xFF382A1A), RoundedCornerShape(4.dp))
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

                // "FM" Label ganz links
                val fmPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 15.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText("FM", 8.dp.toPx(), 26.dp.toPx(), fmPaint)

                // "MHz" Label ganz rechts
                val mhzPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 11.5.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawContext.canvas.nativeCanvas.drawText("MHz", widthPx - 8.dp.toPx(), 25.dp.toPx(), mhzPaint)

                // Frequenzen: 88, 92, 96, 100, 104, 108
                val freqNumbers = listOf(88, 92, 96, 100, 104, 108)
                val numberPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 12.5.dp.toPx()
                    color = textColor
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (num in freqNumbers) {
                    val frac = (num.toFloat() - minFreq) / (maxFreq - minFreq)
                    val x = leftMargin + frac * usableW
                    drawContext.canvas.nativeCanvas.drawText(num.toString(), x, 25.dp.toPx(), numberPaint)
                }

                // Horizontale Skalen-Basislinie
                val baseLineY = 32.dp.toPx()
                drawLine(
                    color = tickColor.copy(alpha = if (isPoweredOn) 0.8f else 0.4f),
                    start = Offset(leftMargin, baseLineY),
                    end = Offset(widthPx - rightMargin, baseLineY),
                    strokeWidth = 1.2.dp.toPx()
                )

                // Teilstriche (Ticks)
                val totalSteps = (maxFreq - minFreq) / 0.5f // alle 0.5 MHz ein Tick
                for (step in 0..totalSteps.toInt()) {
                    val f = minFreq + step * 0.5f
                    val frac = (f - minFreq) / (maxFreq - minFreq)
                    val x = leftMargin + frac * usableW

                    val isMajor = f in listOf(88f, 92f, 96f, 100f, 104f, 108f)
                    val isHalf = (f % 2f == 0f) && !isMajor

                    val tickHeight = when {
                        isMajor -> 10.dp.toPx()
                        isHalf -> 6.5.dp.toPx()
                        else -> 4.dp.toPx()
                    }

                    drawLine(
                        color = tickColor.copy(alpha = if (isMajor) 0.95f else if (isHalf) 0.75f else 0.5f),
                        start = Offset(x, baseLineY),
                        end = Offset(x, baseLineY + tickHeight),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Glühende rote Skalennadel (Vertikaler Leuchtstrich)
                if (isPoweredOn) {
                    // Atmosphärischer weicher Glow
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFF3D00).copy(alpha = 0.45f),
                                Color(0xFFFF1744).copy(alpha = 0.8f),
                                Color(0xFFFF3D00).copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            startX = needleX - 6.dp.toPx(),
                            endX = needleX + 6.dp.toPx()
                        ),
                        topLeft = Offset(needleX - 6.dp.toPx(), 4.dp.toPx()),
                        size = Size(12.dp.toPx(), heightPx - 8.dp.toPx())
                    )

                    // Scharfer roter Leuchtfaden im Zentrum
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = Offset(needleX, 3.dp.toPx()),
                        end = Offset(needleX, heightPx - 3.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                } else {
                    // Abgeschaltet: matte graue Nadel
                    drawLine(
                        color = Color(0xFF6B7280),
                        start = Offset(needleX, 6.dp.toPx()),
                        end = Offset(needleX, heightPx - 6.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // 2. Horizontale Schieber-Führungsnut mit geriffeltem Chrom-Daumenschieber
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF090A0C))
                .border(1.dp, Color(0xFF1E2127), RoundedCornerShape(3.dp))
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

            // Horizontale Vertiefungslinie der Führungsschiene
            Canvas(modifier = Modifier.matchParentSize()) {
                val midY = size.height / 2f
                drawLine(
                    color = Color(0xFF040405),
                    start = Offset(6.dp.toPx(), midY),
                    end = Offset(size.width - 6.dp.toPx(), midY),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF2B303C).copy(alpha = 0.5f),
                    start = Offset(6.dp.toPx(), midY + 1.5.dp.toPx()),
                    end = Offset(size.width - 6.dp.toPx(), midY + 1.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Geriffelter Chrom-Schieber (Knurled Metallic Thumb Slider Button)
            Box(
                modifier = Modifier
                    .offset { IntOffset(sliderLeftPx.roundToInt(), (constraints.maxHeight / 2 - sliderHeight.toPx() / 2).roundToInt()) }
                    .size(width = sliderWidth, height = sliderHeight)
                    .shadow(3.dp, RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE6EBF5),
                                Color(0xFFBDC5D4),
                                Color(0xFF8F98A8),
                                Color(0xFFCAD2E0)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF6E7788), RoundedCornerShape(3.dp))
            ) {
                // Vertikale Riffeln auf dem Schieber wie im Foto
                Canvas(modifier = Modifier.matchParentSize()) {
                    val ridgeCount = 6
                    val step = size.width / (ridgeCount + 1)
                    for (i in 1..ridgeCount) {
                        val rx = i * step
                        // Rille dunkel
                        drawLine(
                            color = Color(0xFF4A5260),
                            start = Offset(rx, 3.dp.toPx()),
                            end = Offset(rx, size.height - 3.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx()
                        )
                        // Rille Lichtkante
                        drawLine(
                            color = Color(0xFFFFFFFF).copy(alpha = 0.8f),
                            start = Offset(rx + 1f, 3.dp.toPx()),
                            end = Offset(rx + 1f, size.height - 3.dp.toPx()),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}
