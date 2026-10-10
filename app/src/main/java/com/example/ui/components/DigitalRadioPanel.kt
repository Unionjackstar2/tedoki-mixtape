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
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onPanelClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRotateKnob: (Float) -> Unit,
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

    val panelBorderColor = if (isDimmed || isTapeRunning) Color(0xFF14161C) else Color(0xFF2E323A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(panelBackground)
            .border(1.5.dp, panelBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
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
                    // STEREO LED indicator (OFF when radio is dimmed / turned off)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isDimmed) LedStereoOff else LedStereoGreen)
                        )
                        Text(
                            text = "STEREO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDimmed) Color(0xFF37474F) else LedStereoGreen
                        )
                    }

                    // REC LED indicator (intense glow if recording)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) LedRecRedGlow else LedRecOff
                                )
                                .then(
                                    if (isRecording) {
                                        Modifier.border(1.dp, Color.White, CircleShape)
                                    } else Modifier
                                )
                        )
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
                // Left preset arrow [<]
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDimmed) Color(0xFF121418) else Color(0xFF1E2127))
                        .border(1.dp, if (isDimmed) Color(0xFF22262E) else Color(0xFF383C45), RoundedCornerShape(4.dp))
                        .clickable(enabled = !isDimmed) { onPreviousStation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Vorheriger Sender",
                        tint = if (isDimmed) Color(0xFF37474F) else Color(0xFFB0BEC5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right preset arrow [>]
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDimmed) Color(0xFF121418) else Color(0xFF1E2127))
                        .border(1.dp, if (isDimmed) Color(0xFF22262E) else Color(0xFF383C45), RoundedCornerShape(4.dp))
                        .clickable(enabled = !isDimmed) { onNextStation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Nächster Sender",
                        tint = if (isDimmed) Color(0xFF37474F) else Color(0xFFB0BEC5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Middle: Station Logo badge + Name & Song info
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPanelClick() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Station real logo (Coil AsyncImage) or fallback badge pill
                    StationLogoView(
                        station = station,
                        modifier = Modifier
                            .size(width = 44.dp, height = 30.dp)
                            .alpha(displayAlpha),
                        fallbackTextSize = 10.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(displayAlpha)
                    ) {
                        Text(
                            text = station?.name ?: "1LIVE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDimmed || isTapeRunning) Color(0xFF455A64) else DigitalLcdText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isTapeRunning -> "[Kassette läuft - Radio aus]"
                                isDimmed -> "[Radio aus - Tippen zum Einschalten]"
                                else -> nowPlayingText.ifBlank { station?.slogan ?: "Live Radio" }
                            },
                            fontSize = 11.sp,
                            color = if (isDimmed || isTapeRunning) Color(0xFF37474F) else DigitalLcdSubtext,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Radio Play / Stop button
                IconButton(
                    onClick = onToggleRadioPlayback,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDimmed) Color(0xFF1E2127)
                            else if (isRadioPlaying) Color(0xFF2E7D32)
                            else Color(0xFFE65100)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDimmed) Color(0xFF383C45)
                            else if (isRadioPlaying) Color(0xFF81C784)
                            else Color(0xFFFFB74D),
                            shape = CircleShape
                        )
                        .testTag("radio_play_stop_button")
                ) {
                    Icon(
                        imageVector = if (isRadioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRadioPlaying) "Radio anhalten" else "Radio wiedergeben",
                        tint = if (isDimmed) Color(0xFF90A4AE) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Favorite star button
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = !isDimmed,
                    modifier = Modifier.size(32.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Rotary metallic Tuning Knob
                Box(modifier = Modifier.alpha(if (isDimmed) 0.5f else 1.0f)) {
                    TuningKnob(
                        onRotateDelta = { delta ->
                            onRotateKnob(delta)
                            if (delta > 20f) {
                                onNextStation()
                            } else if (delta < -20f) {
                                onPreviousStation()
                            }
                        }
                    )
                }
            }
        }
    }
}
