package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ShareTechMonoFontFamily

/**
 * Vintage Hi-Fi Analog Frequency Slider Control.
 * Positioned directly below the FM scale to allow tactile frequency tuning
 * across the FM band (88.0 - 108.0 MHz).
 *
 * Features:
 * - Recessed dark chassis track with vintage tick marks and ruler grooves
 * - "88" and "108" frequency limits on left and right
 * - Brushed aluminum metallic fader knob with an authentic red center indicator stripe
 * - Direct tactile synchronization with the red tuning needle and station search
 */
@Composable
fun FrequencySlider(
    currentFrequencyMhz: Float,
    onFrequencyChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    minFreq: Float = 88.0f,
    maxFreq: Float = 108.0f
) {
    val labelColor = Color(0xFF8E95A2)

    // Outer bezel tray (permanent physical hardware slider, always fully visible with 3D beveled frame)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF20232B),
                        Color(0xFF14161B),
                        Color(0xFF0C0E11)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF454B58), Color(0xFF1D2028))
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left frequency limit label: "88" (with debossed metal look)
            Text(
                text = "${minFreq.toInt()}",
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = labelColor,
                letterSpacing = 0.5.sp
            )

            // Recessed horizontal slider channel with ruler ticks & metallic knob
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF030405),
                                Color(0xFF0A0C0F),
                                Color(0xFF050608)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF1C1F28), RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val thumbW = 32.dp.toPx()
                            val usableWidth = (size.width - thumbW).coerceAtLeast(1f)
                            val fraction = ((offset.x - thumbW / 2f) / usableWidth).coerceIn(0f, 1f)
                            val newFreq = minFreq + fraction * (maxFreq - minFreq)
                            onFrequencyChange(newFreq)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val thumbW = 32.dp.toPx()
                                val usableWidth = (size.width - thumbW).coerceAtLeast(1f)
                                val fraction = ((offset.x - thumbW / 2f) / usableWidth).coerceIn(0f, 1f)
                                val newFreq = minFreq + fraction * (maxFreq - minFreq)
                                onFrequencyChange(newFreq)
                            },
                            onDrag = { change, _ ->
                                val thumbW = 32.dp.toPx()
                                val usableWidth = (size.width - thumbW).coerceAtLeast(1f)
                                val fraction = ((change.position.x - thumbW / 2f) / usableWidth).coerceIn(0f, 1f)
                                val newFreq = minFreq + fraction * (maxFreq - minFreq)
                                onFrequencyChange(newFreq)
                            }
                        )
                    }
                    .testTag("frequency_slider_track"),
                contentAlignment = Alignment.CenterStart
            ) {
                val trackWidth = maxWidth
                val thumbWidth = 32.dp
                val maxOffset = (trackWidth - thumbWidth).coerceAtLeast(0.dp)

                val fraction = ((currentFrequencyMhz - minFreq) / (maxFreq - minFreq)).coerceIn(0f, 1f)
                val thumbOffset = maxOffset * fraction

                // 1. Center recessed groove and fine ruler tick marks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Horizontal central dark guide slit
                    drawLine(
                        color = Color(0xFF020304),
                        start = Offset(6.dp.toPx(), h / 2f),
                        end = Offset(w - 6.dp.toPx(), h / 2f),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Fine vertical tick marks along track
                    val ticks = 30
                    val tickMargin = 8.dp.toPx()
                    val tickWidth = w - tickMargin * 2
                    for (i in 0..ticks) {
                        val tickX = tickMargin + (i.toFloat() / ticks) * tickWidth
                        val isMajor = i % 5 == 0
                        val tickH = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                        val tickColor = if (isMajor) Color(0xFF383E4A) else Color(0xFF20242B)

                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, (h - tickH) / 2f),
                            end = Offset(tickX, (h + tickH) / 2f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // 2. Heavy extruded brushed aluminum slider thumb (Fader Knob)
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .padding(vertical = 1.dp)
                        .shadow(elevation = 5.dp, shape = RoundedCornerShape(3.5.dp))
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFE2E8F0),
                                    Color(0xFFA0AEC0),
                                    Color(0xFF4A5568),
                                    Color(0xFF2D3748)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFF475569))
                            ),
                            RoundedCornerShape(3.5.dp)
                        )
                        .testTag("frequency_slider_thumb"),
                    contentAlignment = Alignment.Center
                ) {
                    // Top specular reflection line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .background(Color(0xCCFFFFFF))
                    )

                    // Tactile milled flutes and bright ruby indicator stripe
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                    ) {
                        // Left tactile groove
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight(0.68f)
                                .background(Color(0xFF1A202C), RoundedCornerShape(0.5.dp))
                        )

                        // Center Red Indicator Stripe (matching vintage tuner pointer with 3D acrylic depth)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(0.90f)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFF8A80),
                                            Color(0xFFFF1744),
                                            Color(0xFFD50000),
                                            Color(0xFF8B0000)
                                        )
                                    )
                                )
                                .border(0.5.dp, Color(0xFF2B0004), RoundedCornerShape(1.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glossy core line inside red stripe
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.85f)
                                    .background(Color(0x99FFFFFF))
                            )
                        }

                        // Right tactile groove
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight(0.68f)
                                .background(Color(0xFF1A202C), RoundedCornerShape(0.5.dp))
                        )
                    }
                }
            }

            // Right frequency limit label: "108"
            Text(
                text = "${maxFreq.toInt()}",
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
