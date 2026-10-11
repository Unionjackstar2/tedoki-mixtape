package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeckState
import com.example.ui.theme.ButtonKeyBorder
import com.example.ui.theme.ButtonKeySurface
import com.example.ui.theme.ButtonKeySurfacePressed
import com.example.ui.theme.ButtonKeyText
import com.example.ui.theme.LedRecRed
import com.example.ui.theme.LedRecRedGlow

@Composable
fun TransportControls(
    deckState: DeckState,
    onRecordClick: () -> Unit,
    onPlayClick: () -> Unit,
    onRewindClick: () -> Unit,
    onFastForwardClick: () -> Unit,
    onPauseClick: () -> Unit,
    onEjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Recessed keyboard well tray with bevelled edge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0D10),
                        Color(0xFF131519),
                        Color(0xFF0F1014)
                    )
                )
            )
            .border(1.5.dp, Color(0xFF262A33), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        val isRecordingActive = deckState == DeckState.RECORDING || deckState == DeckState.RECORD_ARMED ||
                deckState == DeckState.RECORDING_PAUSED || deckState == DeckState.RECORDING_STARTING || deckState == DeckState.RECORDING_STOPPING
        val isPlayingActive = deckState == DeckState.PLAYING || deckState == DeckState.RECORDING ||
                deckState == DeckState.RECORDING_STARTING || deckState == DeckState.RECORDING_STOPPING
        val isRewindActive = deckState == DeckState.REWINDING
        val isFastForwardActive = deckState == DeckState.FAST_FORWARDING
        val isPauseActive = deckState == DeckState.PAUSED
        val isEjectActive = deckState == DeckState.EJECTED

        // 1. RECORD
        MechanicalKey(
            label = "RECORD",
            isActive = isRecordingActive,
            isRecordKey = true,
            testTag = "transport_record",
            onClick = onRecordClick,
            modifier = Modifier.weight(1f)
        ) {
            // Inset 3D ruby jewel record dot with metallic bezel ring
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A0A0D))
                    .border(1.dp, Color(0xFF4E1419), CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecordingActive) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFFF5252),
                                        Color(0xFFD50000),
                                        Color(0xFF8B0000)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF8A80),
                                        Color(0xFFE53935),
                                        Color(0xFFB71C1C)
                                    )
                                )
                            }
                        )
                        .border(
                            0.5.dp,
                            if (isRecordingActive) Color.White.copy(alpha = 0.9f) else Color(0x66FFFFFF),
                            CircleShape
                        )
                )
            }
        }

        // 2. PLAY
        MechanicalKey(
            label = "PLAY",
            isActive = isPlayingActive,
            testTag = "transport_play",
            onClick = onPlayClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "▶",
                fontSize = 14.sp,
                color = if (isPlayingActive) Color(0xFFFFB300) else ButtonKeyText
            )
        }

        // 3. REWIND (◄◄)
        MechanicalKey(
            label = "REW",
            isActive = isRewindActive,
            testTag = "transport_rewind",
            onClick = onRewindClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "◄◄",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRewindActive) Color(0xFFFFB300) else ButtonKeyText
            )
        }

        // 4. FAST FORWARD (►►)
        MechanicalKey(
            label = "F.FWD",
            isActive = isFastForwardActive,
            testTag = "transport_fast_forward",
            onClick = onFastForwardClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "►►",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFastForwardActive) Color(0xFFFFB300) else ButtonKeyText
            )
        }

        // 5. PAUSE
        MechanicalKey(
            label = "PAUSE",
            isActive = isPauseActive,
            testTag = "transport_pause",
            onClick = onPauseClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "❚❚",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPauseActive) Color(0xFFFFB300) else ButtonKeyText
            )
        }

        // 6. STOP / EJECT
        MechanicalKey(
            label = "STOP / EJECT",
            isActive = isEjectActive,
            testTag = "transport_eject",
            onClick = onEjectClick,
            modifier = Modifier.weight(1.1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "■",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ButtonKeyText
                )
                Text(
                    text = "⏏",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ButtonKeyText
                )
            }
        }
        }
    }
}

@Composable
fun MechanicalKey(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isRecordKey: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed || isActive) 3.dp else 0.dp,
        label = "pressDepth"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Individual recessed key-well slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF07080A))
                .border(0.5.dp, Color(0xFF1C1E24), RoundedCornerShape(4.dp))
                .padding(horizontal = 0.5.dp, vertical = 1.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Extruded Mechanical Piano Key
            Box(
                modifier = Modifier
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                    .fillMaxWidth()
                    .height(38.dp)
                    .offset(y = pressOffset)
                    .shadow(
                        elevation = if (isActive || isPressed) 1.dp else 5.dp,
                        shape = RoundedCornerShape(3.5.dp)
                    )
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(
                        if (isRecordKey && isActive) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF8B1D23),
                                    Color(0xFF5B1015),
                                    Color(0xFF38080C)
                                )
                            )
                        } else if (isActive || isPressed) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1F2228),
                                    Color(0xFF16181C),
                                    Color(0xFF101215)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4C525E),
                                    Color(0xFF2E323A),
                                    Color(0xFF22252C),
                                    Color(0xFF181A1F)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isRecordKey && isActive) {
                            Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFF900C14)))
                        } else if (isActive || isPressed) {
                            Brush.verticalGradient(listOf(Color(0xFF3A3F49), Color(0xFF15181C)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF717A8A), Color(0xFF2C3038)))
                        },
                        shape = RoundedCornerShape(3.5.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Top specular highlight line on key cap
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isRecordKey && isActive) Color(0xFFFF8A80) else Color(0x99A0AEC0),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom tactile step shadow inside key face
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-1).dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x99000000),
                                    Color.Transparent
                                )
                            )
                        )
                )

                icon()
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            color = if (isActive) Color(0xFFFFB300) else Color(0xFF8B94A0),
            letterSpacing = 0.2.sp
        )
    }
}
