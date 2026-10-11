package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.ui.theme.DigitalLcdBackground
import com.example.ui.theme.DigitalLcdSubtext
import com.example.ui.theme.DigitalLcdText
import com.example.ui.theme.LedRecOff
import com.example.ui.theme.LedRecRed
import com.example.ui.theme.LedRecRedGlow
import com.example.ui.theme.LedStereoGreen
import com.example.ui.theme.LedStereoOff

@Composable
fun DigitalRadioPanel(
    station: RadioStation?,
    nowPlayingText: String,
    isRecording: Boolean,
    isFavorite: Boolean = station?.isFavorite == true,
    isDimmed: Boolean = false,
    isTapeRunning: Boolean = false,
    isRadioPlaying: Boolean = false,
    onToggleRadioPlayback: () -> Unit = {},
    onPreviousStation: () -> Unit = {},
    onNextStation: () -> Unit = {},
    onPanelClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRotateKnob: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayAlpha by animateFloatAsState(
        targetValue = if (isTapeRunning) 0.05f else if (isDimmed) 0.12f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "displayAlpha"
    )

    val panelBackground = if (isDimmed || isTapeRunning) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF07080A),
                Color(0xFF040506),
                Color(0xFF07080A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DigitalLcdBackground,
                Color(0xFF0E1013),
                DigitalLcdBackground
            )
        )
    }

    val panelBorderBrush = if (isDimmed || isTapeRunning) {
        Brush.verticalGradient(listOf(Color(0xFF1E2129), Color(0xFF0F1115)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF485060), Color(0xFF1F232B)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(panelBackground)
            .border(1.5.dp, panelBorderBrush, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top status line: "DAB+ / Internet Radio" | "• STEREO" | "• REC"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isTapeRunning -> "DAB+ / TUNER  [OFF - KASSETTE LÄUFT]"
                        isDimmed -> "DAB+ / TUNER  [STANDBY / OFF]"
                        else -> "DAB+ / Internet Radio"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDimmed || isTapeRunning) Color(0xFF37474F) else DigitalLcdSubtext
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // STEREO LED indicator with 3D chrome collar bezel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF181B20))
                                .border(0.5.dp, Color(0xFF4B5362), CircleShape)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isDimmed) {
                                            Brush.radialGradient(listOf(LedStereoOff, Color(0xFF0A140D)))
                                        } else {
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFE8F5E9),
                                                    LedStereoGreen,
                                                    Color(0xFF00796B)
                                                )
                                            )
                                        }
                                    )
                            )
                        }
                        Text(
                            text = "STEREO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDimmed) Color(0xFF37474F) else LedStereoGreen
                        )
                    }

                    // REC LED indicator with 3D chrome collar bezel and intense glow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1113))
                                .border(0.5.dp, if (isRecording) Color(0xFFFF5252) else Color(0xFF4D1C22), CircleShape)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecording) {
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFFFFFFF),
                                                    LedRecRedGlow,
                                                    LedRecRed,
                                                    Color(0xFF8B0000)
                                                )
                                            )
                                        } else {
                                            Brush.radialGradient(listOf(LedRecOff, Color(0xFF1A0A0C)))
                                        }
                                    )
                                    .then(
                                        if (isRecording) {
                                            Modifier.border(0.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                        } else Modifier
                                    )
                            )
                        }
                        Text(
                            text = "REC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) LedRecRed else Color(0xFF757575)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main station display row with knob
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Middle: Vintage 10-Digit 14-Segment / Matrix Radio Display (Sanftes Orange)
                RetroRadioDisplay(
                    stationName = station?.name ?: "1LIVE",
                    subtext = when {
                        isTapeRunning -> "[Kassette läuft - Radio aus]"
                        isDimmed -> "[Radio aus - Tippen zum Einschalten]"
                        else -> {
                            val freqStr = station?.let { "${it.dialFrequencyMhz} MHz • " } ?: ""
                            val info = nowPlayingText.ifBlank { station?.slogan ?: "Live Radio" }
                            "$freqStr$info"
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .alpha(displayAlpha),
                    isDimmed = isDimmed || isTapeRunning,
                    onDisplayClick = onPanelClick
                )

                // Favorite star button
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = !isDimmed,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorit",
                        tint = if (isDimmed) {
                            Color(0xFF37474F)
                        } else if (isFavorite) {
                            Color(0xFFFFB300)
                        } else {
                            Color(0xFF78909C)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
