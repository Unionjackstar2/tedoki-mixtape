package de.tedoki.mixtape.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import de.tedoki.mixtape.ui.theme.AmberDialNeedle
import de.tedoki.mixtape.ui.theme.AmberDisplayBackground
import de.tedoki.mixtape.ui.theme.AmberScaleSub
import de.tedoki.mixtape.ui.theme.AmberScaleText
import de.tedoki.mixtape.util.HapticUtils
import kotlin.math.roundToInt

@Composable
fun TuningScale(
    currentFrequencyMhz: Float,
    onFrequencyChange: (Float) -> Unit,
    isDimmed: Boolean = false,
    isSilverStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val minFreq = 88.0f
    val maxFreq = 108.0f

    val scaleAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.08f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "scaleAlpha"
    )

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isDimmed) {
                        listOf(
                            Color(0xFF080604),
                            Color(0xFF0C0906),
                            Color(0xFF080604)
                        )
                    } else {
                        listOf(
                            Color(0xFF1F1405),
                            Color(0xFF2C1C08),
                            Color(0xFF140D04)
                        )
                    }
                )
            )
            .border(
                1.5.dp,
                if (isSilverStyle) {
                    if (isDimmed) {
                        Brush.verticalGradient(listOf(Color(0xFF8B95A5), Color(0xFF4A5568)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFC0CAD8), Color(0xFF788395)))
                    }
                } else {
                    Brush.verticalGradient(
                        if (isDimmed) listOf(Color(0xFF1C140A), Color(0xFF1C140A))
                        else listOf(Color(0xFF5A3E1B), Color(0xFF33200B))
                    )
                },
                RoundedCornerShape(6.dp)
            )
            .pointerInput(isDimmed) {
                if (!isDimmed) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val newFreq = minFreq + fraction * (maxFreq - minFreq)
                        updateFrequencyWithHaptics(newFreq)
                    }
                }
            }
            .pointerInput(isDimmed) {
                if (!isDimmed) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            updateFrequencyWithHaptics(newFreq)
                        }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(scaleAlpha)
        ) {
            val width = size.width
            val height = size.height

            // Background subtle illuminated warm grid glow with dual bulb lamps
            if (!isDimmed) {
                // Left lamp glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x35FFA000), Color.Transparent),
                        center = Offset(width * 0.25f, height * 0.4f),
                        radius = width * 0.35f
                    )
                )
                // Right lamp glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x35FFA000), Color.Transparent),
                        center = Offset(width * 0.75f, height * 0.4f),
                        radius = width * 0.35f
                    )
                )
            }

            // Top inner shadow cast into the recessed glass cavity
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x99000000), Color.Transparent),
                    startY = 0f,
                    endY = 10.dp.toPx()
                )
            )

            // FM text on the left with 3D embossed shadow
            drawContext.canvas.nativeCanvas.apply {
                val shadowPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#33000000")
                    textSize = 34f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("FM", 26f, height * 0.53f, shadowPaint)

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFB300")
                    textSize = 34f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("FM", 24f, height * 0.52f, paint)
            }

            // Frequency ticks & labels: 88, 92, 96, 100, 104, 108
            val majorFreqs = listOf(88, 92, 96, 100, 104, 108)
            val startX = width * 0.20f
            val endX = width * 0.94f
            val scaleWidth = endX - startX

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFB300")
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            // Draw ticks across scale
            val totalSteps = 40
            for (step in 0..totalSteps) {
                val fraction = step.toFloat() / totalSteps
                val x = startX + fraction * scaleWidth
                val isMajor = step % 8 == 0
                val isMedium = step % 4 == 0

                val tickHeight = when {
                    isMajor -> 18.dp.toPx()
                    isMedium -> 12.dp.toPx()
                    else -> 7.dp.toPx()
                }

                drawLine(
                    color = if (isMajor) AmberScaleText else AmberScaleSub.copy(alpha = 0.7f),
                    start = Offset(x, height * 0.72f),
                    end = Offset(x, height * 0.72f - tickHeight),
                    strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx()
                )
            }

            // Draw major numbers above ticks
            for (freq in majorFreqs) {
                val fraction = (freq - minFreq) / (maxFreq - minFreq)
                val x = startX + fraction * scaleWidth
                drawContext.canvas.nativeCanvas.drawText("$freq", x, height * 0.34f, textPaint)
            }

            // Illuminated Red Needle marker with 3D physical cast shadow
            val needleFraction = ((currentFrequencyMhz - minFreq) / (maxFreq - minFreq)).coerceIn(0f, 1f)
            val needleX = startX + needleFraction * scaleWidth

            // Physical shadow cast onto the dial plate behind the needle
            drawLine(
                color = Color(0x66000000),
                start = Offset(needleX + 2.dp.toPx(), 7.dp.toPx()),
                end = Offset(needleX + 2.dp.toPx(), height - 5.dp.toPx()),
                strokeWidth = 3.dp.toPx()
            )

            // Needle diffuse glow
            drawLine(
                color = AmberDialNeedle.copy(alpha = 0.35f),
                start = Offset(needleX, 6.dp.toPx()),
                end = Offset(needleX, height - 6.dp.toPx()),
                strokeWidth = 6.dp.toPx()
            )

            // Needle solid blade
            drawLine(
                color = AmberDialNeedle,
                start = Offset(needleX, 6.dp.toPx()),
                end = Offset(needleX, height - 6.dp.toPx()),
                strokeWidth = 2.5.dp.toPx()
            )

            // Needle bright specular center light pipe
            drawLine(
                color = Color(0xCCFFFFFF),
                start = Offset(needleX, 9.dp.toPx()),
                end = Offset(needleX, height - 9.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Realistic diagonal specular glass reflection streak across dial lens
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x00FFFFFF),
                        Color(0x18FFFFFF),
                        Color(0x28FFFFFF),
                        Color(0x00FFFFFF)
                    ),
                    start = Offset(0f, height),
                    end = Offset(width * 0.6f, 0f)
                ),
                start = Offset(0f, height),
                end = Offset(width * 0.6f, 0f),
                strokeWidth = 24.dp.toPx()
            )
        }
    }
}
