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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isRecordingActive = deckState == DeckState.RECORDING || deckState == DeckState.RECORD_ARMED || deckState == DeckState.RECORDING_PAUSED
        val isPlayingActive = deckState == DeckState.PLAYING || deckState == DeckState.RECORDING
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
            // Red record circle dot
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecordingActive) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    LedRecRedGlow,
                                    LedRecRed
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEF5350),
                                    Color(0xFFC62828)
                                )
                            )
                        }
                    )
                    .then(
                        if (isRecordingActive) {
                            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        } else Modifier
                    )
            )
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
        Box(
            modifier = Modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = pressOffset)
                .shadow(
                    elevation = if (isActive || isPressed) 1.dp else 5.dp,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isRecordKey && isActive) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6B1D22),
                                Color(0xFF381014)
                            )
                        )
                    } else if (isActive || isPressed) {
                        Brush.verticalGradient(
                            colors = listOf(
                                ButtonKeySurfacePressed,
                                Color(0xFF15171B)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF383C45),
                                ButtonKeySurface,
                                Color(0xFF1E2025)
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isRecordKey && isActive) Color(0xFFFF5252) else ButtonKeyBorder,
                    shape = RoundedCornerShape(4.dp)
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
            icon()
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            color = if (isActive) Color(0xFFFFB300) else Color(0xFF78909C),
            letterSpacing = 0.2.sp
        )
    }
}
