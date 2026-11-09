package de.tedoki.mixtape.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun TuningKnob(
    onRotateDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(45f) }
    val animatedAngle by animateFloatAsState(targetValue = rotationAngle, label = "knobAngle")

    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF5A606D),
                        Color(0xFF383C45),
                        Color(0xFF22252B)
                    )
                )
            )
            .border(2.dp, Color(0xFF1B1D22), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.y * -0.8f + dragAmount.x * 0.8f
                    rotationAngle += delta
                    onRotateDelta(delta)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Rotating knob face with knurled ridges & indicator notch
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .rotate(animatedAngle)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF4A4E57),
                            Color(0xFF7B8391),
                            Color(0xFF32363D),
                            Color(0xFF7B8391),
                            Color(0xFF4A4E57)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Indicator notch at top
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 2.5.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.72f)
                )

                // Concentric metallic bevel rings
                drawCircle(
                    color = Color(0xFF22252A),
                    radius = radius * 0.45f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF5A606D),
                    radius = radius * 0.38f,
                    center = center
                )
            }
        }
    }
}
