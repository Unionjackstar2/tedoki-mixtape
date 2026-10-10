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
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0D0E10))
            .border(1.5.dp, Color(0xFF3A3F47), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            digits.forEach { digit ->
                Box(
                    modifier = Modifier
                        .size(width = 15.dp, height = 22.dp)
                        .background(Color(0xFF1B1D22), RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color(0xFF2E323A), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECEFF1)
                    )
                }
            }

            if (onReset != null) {
                Spacer(modifier = Modifier.width(3.dp))
                // Tactile round reset push button
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF23272F))
                        .border(1.dp, Color(0xFF4A5160), CircleShape)
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
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
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F1114))
            .border(1.dp, Color(0xFF2A2D35), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF78909C),
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(barColor)
                )
            }
        }
    }
}
