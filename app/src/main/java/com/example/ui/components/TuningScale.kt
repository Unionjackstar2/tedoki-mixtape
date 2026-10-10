package com.example.ui.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberDialNeedle
import com.example.ui.theme.AmberDisplayBackground
import com.example.ui.theme.AmberScaleSub
import com.example.ui.theme.AmberScaleText

@Composable
fun TuningScale(
    currentFrequencyMhz: Float,
    onFrequencyChange: (Float) -> Unit,
    isDimmed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val minFreq = 88.0f
    val maxFreq = 108.0f

    val scaleAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.08f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "scaleAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
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
                            AmberDisplayBackground,
                            Color(0xFF261908),
                            AmberDisplayBackground
                        )
                    }
                )
            )
            .border(1.5.dp, if (isDimmed) Color(0xFF191208) else Color(0xFF422F17), RoundedCornerShape(6.dp))
            .pointerInput(isDimmed) {
                if (!isDimmed) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val newFreq = minFreq + fraction * (maxFreq - minFreq)
                        onFrequencyChange(newFreq)
                    }
                }
            }
            .pointerInput(isDimmed) {
                if (!isDimmed) {
                    detectDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newFreq = minFreq + fraction * (maxFreq - minFreq)
                        onFrequencyChange(newFreq)
                    }
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

            // Background subtle illuminated warm grid glow
            if (!isDimmed) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33FF9800), Color(0x00000000)),
                        center = Offset(width / 2, height / 2),
                        radius = width * 0.6f
                    )
                )
            }

            // FM text on the left
            drawContext.canvas.nativeCanvas.apply {
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

            // Illuminated Red Needle marker
            val needleFraction = ((currentFrequencyMhz - minFreq) / (maxFreq - minFreq)).coerceIn(0f, 1f)
            val needleX = startX + needleFraction * scaleWidth

            // Needle shadow / glow
            drawLine(
                color = AmberDialNeedle.copy(alpha = 0.4f),
                start = Offset(needleX, 6.dp.toPx()),
                end = Offset(needleX, height - 6.dp.toPx()),
                strokeWidth = 6.dp.toPx()
            )
            // Needle solid line
            drawLine(
                color = AmberDialNeedle,
                start = Offset(needleX, 6.dp.toPx()),
                end = Offset(needleX, height - 6.dp.toPx()),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}
