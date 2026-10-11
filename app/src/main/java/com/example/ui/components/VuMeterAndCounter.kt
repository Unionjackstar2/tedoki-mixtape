package com.example.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ShareTechMonoFontFamily
import com.example.ui.theme.VuAmber
import com.example.ui.theme.VuGreen
import com.example.ui.theme.VuInactive
import com.example.ui.theme.VuRed

@Composable
fun VuMeterAndCounter(
    counterValue: Int,
    vuLeft: Int,
    vuRight: Int,
    onResetCounter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mechanical Odometer Counter Box: [ 0 0 0 ]
        MechanicalTapeCounter(
            value = counterValue,
            onReset = onResetCounter
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Stereo Dual VU-Meter (L and R)
        DualVuMeter(
            levelLeft = vuLeft,
            levelRight = vuRight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MechanicalTapeCounter(
    value: Int,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null
) {
    val padded = String.format("%03d", value.coerceIn(0, 999))
    val digits = padded.map { it.toString() }

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F2228), Color(0xFF121417), Color(0xFF0B0C0E))
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4C525E), Color(0xFF1E2128))
                ),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 5.dp, vertical = 3.5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            digits.forEach { digit ->
                // Cylindrical odometer number drum
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF090A0D),
                                    Color(0xFF1F232B),
                                    Color(0xFF2E333D),
                                    Color(0xFF1C2027),
                                    Color(0xFF07080A)
                                )
                            )
                        )
                        .border(0.5.dp, Color(0xFF383E49), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Drum center highlight line behind numeral
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    Text(
                        text = digit,
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF1F5F9)
                    )
                }
            }

            if (onReset != null) {
                Spacer(modifier = Modifier.width(3.5.dp))
                // 3D Tactile spring-loaded reset push button
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3D4452),
                                    Color(0xFF22262E),
                                    Color(0xFF15171C)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF6B7485), Color(0xFF242730))
                            ),
                            CircleShape
                        )
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}

@Composable
fun DualVuMeter(
    levelLeft: Int,
    levelRight: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08090C),
                        Color(0xFF111418),
                        Color(0xFF090A0D)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E4552), Color(0xFF1C1F26))
                ),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Screen-printed dB scale header line above channels
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp, bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("-20", "-10", "-5", "-1", "0", "+3", "+6").forEach { db ->
                Text(
                    text = db,
                    fontSize = 7.5.sp,
                    fontFamily = ShareTechMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (db.startsWith("+")) Color(0xFFFF5252) else if (db == "0") Color(0xFFFFC107) else Color(0xFF78909C)
                )
            }
        }

        VuChannelRow(label = "L", activeBars = levelLeft)
        VuChannelRow(label = "R", activeBars = levelRight)
    }
}

@Composable
private fun VuChannelRow(
    label: String,
    activeBars: Int
) {
    val totalBars = 16

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF90A4AE),
            modifier = Modifier.width(13.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 1..totalBars) {
                val isActive = i <= activeBars
                val barColor = when {
                    !isActive -> VuInactive
                    i <= 10 -> VuGreen
                    i <= 13 -> VuAmber
                    else -> VuRed
                }

                // 3D beveled LED light pipe segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isActive) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.7f),
                                        barColor,
                                        barColor.copy(alpha = 0.85f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2C3038),
                                        Color(0xFF1A1C22)
                                    )
                                )
                            }
                        )
                        .border(
                            0.5.dp,
                            if (isActive) barColor else Color(0xFF14161C),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
