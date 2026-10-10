package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Cassette
import com.example.ui.theme.CassetteInkBlue
import com.example.ui.theme.CassetteLabelPaper
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily
import com.example.ui.theme.TapeColorBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Authentic Compact Audio Cassette Component:
 * - 3D plastic molded reliefs (stepped shell, top grip ribs, write-protect notches, embossed A/B badges)
 * - 5 realistic metallic crosshead screws with countersunk bevels
 * - Corner guide rollers (Umlenkrollen) with nylon pulleys and metallic axle pins
 * - Authentic round reels with 6 inward-pointing drive teeth (Zähnchen), tape clamp pin, and spoke cutouts
 * - Physical volume-conserving magnetic tape packs that dynamically increase/decrease during recording and playback
 * - Center acrylic bay window with 100-50-0 ruler markings and reflective sheen
 * - Customizable paper label with handwriting title and retro accents
 */
@Composable
fun CassetteView(
    cassette: Cassette,
    isSpinning: Boolean = false,
    isRewinding: Boolean = false,
    isFastForwarding: Boolean = false,
    tapePositionSec: Int = cassette.currentPositionSec,
    totalCapacitySec: Int = cassette.totalCapacitySec,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null
) {
    val shellColor = try {
        Color(android.graphics.Color.parseColor(cassette.colorHex))
    } catch (e: Exception) {
        TapeColorBlack
    }

    // Dynamic tape progress (0.0 at beginning, 1.0 at end of tape)
    val safeCapacity = totalCapacitySec.coerceAtLeast(60)
    val progress = (tapePositionSec.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 1f)

    // Reel hub geometry and volume conservation
    val rHub = 0.50f // empty hub radius ratio
    val rMax = 0.94f // full reel radius ratio
    val deltaArea = rMax * rMax - rHub * rHub
    // As tape moves forward (playing or recording), left spool unwinds and right spool takes up tape!
    val leftPackRadiusRatio = sqrt(rHub * rHub + (1f - progress) * deltaArea)
    val rightPackRadiusRatio = sqrt(rHub * rHub + progress * deltaArea)

    val isMoving = isSpinning || isRewinding || isFastForwarding
    val animDuration = when {
        isRewinding || isFastForwarding -> 360
        else -> 2000
    }
    // Forward playback/record moves tape from supply to take-up (clockwise)
    val initialRot = if (isRewinding) 360f else 0f
    val targetRot = if (isRewinding) 0f else 360f

    val infiniteTransition = rememberInfiniteTransition(label = "reels")
    val reelRotation by infiniteTransition.animateFloat(
        initialValue = if (isMoving) initialRot else 0f,
        targetValue = if (isMoving) targetRot else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelSpin"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1.57f) // Compact Cassette standard form factor (~100.2 x 63.8 mm)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        shellColor,
                        shellColor.copy(alpha = 0.88f),
                        shellColor
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6B7280),
                        Color(0xFF1E222A)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // Canvas for all authentic cassette shell reliefs, screws, guide rollers, and bottom head trapezoid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Top write-protect tabs (Löschschutzlaschen) recessed notches
            val tabWidth = w * 0.065f
            val tabHeight = h * 0.055f
            // Left tab
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.035f, 0f),
                size = Size(tabWidth, tabHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(w * 0.035f, 0f),
                size = Size(tabWidth, tabHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )
            // Right tab
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.90f, 0f),
                size = Size(tabWidth, tabHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(w * 0.90f, 0f),
                size = Size(tabWidth, tabHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )

            // 2. Top-edge textured grip ribs (Riffelung für besseren Griff)
            val ribCount = 16
            val ribStartX = w * 0.28f
            val ribEndX = w * 0.72f
            val ribStep = (ribEndX - ribStartX) / ribCount
            for (i in 0 until ribCount) {
                val rx = ribStartX + i * ribStep
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(rx, 1.dp.toPx()),
                    end = Offset(rx, 7.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(rx + 1.2.dp.toPx(), 1.dp.toPx()),
                    end = Offset(rx + 1.2.dp.toPx(), 7.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // 3. Molded 3D plastic bevel line around cassette perimeter
            val margin = 4.dp.toPx()
            val innerPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = margin,
                        top = margin,
                        right = w - margin,
                        bottom = h - margin,
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                )
            }
            drawPath(
                path = innerPath,
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(1.2.dp.toPx())
            )

            // 4. Embossed Side "A" plastic relief medallion on left shell
            val badgeX = w * 0.07f
            val badgeY = h * 0.36f
            val badgeR = h * 0.055f
            drawCircle(Color.Black.copy(alpha = 0.35f), badgeR, Offset(badgeX, badgeY))
            drawCircle(Color.White.copy(alpha = 0.18f), badgeR, Offset(badgeX - 0.5f, badgeY - 0.5f), style = Stroke(1.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.5f), badgeR, Offset(badgeX + 0.5f, badgeY + 0.5f), style = Stroke(1.dp.toPx()))

            // 5. Bottom Trapezoid Tape Head Guide (Kopfteil)
            val trapLeftBase = w * 0.21f
            val trapLeftTop = w * 0.26f
            val trapRightTop = w * 0.74f
            val trapRightBase = w * 0.79f
            val trapTopY = h * 0.835f

            val trapPath = Path().apply {
                moveTo(trapLeftBase, h)
                lineTo(trapLeftTop, trapTopY)
                lineTo(trapRightTop, trapTopY)
                lineTo(trapRightBase, h)
                close()
            }
            // Shaded trapezoid plate
            drawPath(
                path = trapPath,
                color = Color.Black.copy(alpha = 0.38f)
            )
            // Stepped 3D highlight bevel along trapezoid boundary
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(trapLeftBase, h),
                end = Offset(trapLeftTop, trapTopY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(trapLeftTop, trapTopY),
                end = Offset(trapRightTop, trapTopY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.6f),
                start = Offset(trapRightTop, trapTopY),
                end = Offset(trapRightBase, h),
                strokeWidth = 1.5.dp.toPx()
            )

            // 6. Closed bottom housing (Geschlossenes Gehäuse mit Capstan-Führungen wie im Original)
            val capstanY = h * 0.90f
            val capstanRadius = h * 0.022f
            val capstanLeftX = w * 0.35f
            val capstanRightX = w * 0.65f
            // Left capstan hole
            drawCircle(Color(0xFF0E1116), capstanRadius, Offset(capstanLeftX, capstanY))
            drawCircle(Color.White.copy(alpha = 0.2f), capstanRadius, Offset(capstanLeftX, capstanY), style = Stroke(0.9.dp.toPx()))
            // Right capstan hole
            drawCircle(Color(0xFF0E1116), capstanRadius, Offset(capstanRightX, capstanY))
            drawCircle(Color.White.copy(alpha = 0.2f), capstanRadius, Offset(capstanRightX, capstanY), style = Stroke(0.9.dp.toPx()))

            // 7. Authentic Screws (5 miniature metallic crosshead screws)
            val screwR = 3.6.dp.toPx()
            val cornerOffset = 13.dp.toPx()
            // Top-left
            drawCassetteScrew(Offset(cornerOffset, cornerOffset), screwR, angleDeg = 35f)
            // Top-right
            drawCassetteScrew(Offset(w - cornerOffset, cornerOffset), screwR, angleDeg = 115f)
            // Bottom-left
            drawCassetteScrew(Offset(cornerOffset, h - cornerOffset), screwR, angleDeg = 75f)
            // Bottom-right
            drawCassetteScrew(Offset(w - cornerOffset, h - cornerOffset), screwR, angleDeg = 205f)
            // Center top screw
            drawCassetteScrew(Offset(w * 0.5f, 11.dp.toPx()), screwR, angleDeg = 145f)
        }

        // Inner Sticker / Paper Label (Recessed into the cassette face)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.72f)
                .align(Alignment.TopCenter)
                .offset(y = 12.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(7.dp))
                .clip(RoundedCornerShape(7.dp))
                .background(CassetteLabelPaper)
                .border(1.2.dp, Color(0xFFC4B89F), RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Header row of label: [A] Badge, Handwritten Title, [60] length badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onLabelClick != null) { onLabelClick?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Retro Side Badge [A]
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.2.dp, Color(0xFF2B2B2B), RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cassette.sideA.take(1).ifBlank { "A" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1B1B1B)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Mixtape Title in authentic handwritten font
                Text(
                    text = cassette.title.ifBlank { "Mein Mixtape" },
                    fontFamily = CaveatFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CassetteInkBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.width(10.dp))

                // C-60 duration badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "C-60",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF755E43)
                    )
                    Text(
                        text = "STEREO",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF9E8A74),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Vintage decorative accent stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFD32F2F),
                                Color(0xFFFF9800),
                                Color(0xFF1976D2),
                                Color(0xFF388E3C)
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Transparent Center Bay Window with Reels, Tape Packs & Dynamic Tape Volume
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF121418))
                    .border(1.5.dp, Color(0xFF323640), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Acrylic glass reflections & 100 - 50 - 0 tape measurement scale
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Glossy diagonal acrylic glare highlight
                    val highlightPath = Path().apply {
                        moveTo(size.width * 0.12f, 0f)
                        lineTo(size.width * 0.38f, 0f)
                        lineTo(size.width * 0.22f, size.height)
                        lineTo(size.width * 0.02f, size.height)
                        close()
                    }
                    drawPath(highlightPath, Color.White.copy(alpha = 0.05f))

                    // Center measurement ruler ticks
                    val centerY = size.height * 0.5f
                    for (i in -4..4) {
                        val tickX = size.width * 0.5f + (i * 7.5.dp.toPx())
                        val tickH = if (i == 0) 11.dp.toPx() else if (i % 2 == 0) 7.dp.toPx() else 4.dp.toPx()
                        drawLine(
                            color = Color(0xFF6B7280).copy(alpha = 0.8f),
                            start = Offset(tickX, centerY - tickH / 2),
                            end = Offset(tickX, centerY + tickH / 2),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Connecting tape band between spools inside window
                    val bandY = size.height * 0.5f
                    drawLine(
                        color = Color(0xFF261912),
                        start = Offset(size.width * 0.22f, bandY),
                        end = Offset(size.width * 0.78f, bandY),
                        strokeWidth = 5.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF3F2B1F),
                        start = Offset(size.width * 0.22f, bandY - 1f),
                        end = Offset(size.width * 0.78f, bandY - 1f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Ruler indicator text labels (100 ... 50 ... 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.34f)
                        .offset(y = (-14).dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "100",
                        fontSize = 8.sp,
                        fontFamily = ShareTechMonoFontFamily,
                        color = Color(0xFF90A4AE),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "50",
                        fontSize = 8.sp,
                        fontFamily = ShareTechMonoFontFamily,
                        color = Color(0xFF90A4AE),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "0",
                        fontSize = 8.sp,
                        fontFamily = ShareTechMonoFontFamily,
                        color = Color(0xFF90A4AE),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Two Spools with dynamically winding & unwinding tape packs
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Spool (Supply Reel - decreases during recording / playback)
                    CassetteReel(
                        rotation = reelRotation,
                        tapePackRadiusRatio = leftPackRadiusRatio,
                        modifier = Modifier.size(totalHeight * 0.38f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Right Spool (Take-up Reel - increases during recording / playback)
                    CassetteReel(
                        rotation = reelRotation,
                        tapePackRadiusRatio = rightPackRadiusRatio,
                        modifier = Modifier.size(totalHeight * 0.38f)
                    )
                }
            }
        }
    }
}

/**
 * Authentic Cassette Reel (Wickelkern):
 * - Round circular plastic body
 * - Dark oxide magnetic tape pack with concentric winding layers
 * - Leader-tape anchor notch with red clamp pin
 * - 6 circular spoke cutouts
 * - Round central drive hole with 6 distinct inward-pointing drive teeth (Zähnchen!)
 */
@Composable
fun CassetteReel(
    rotation: Float,
    tapePackRadiusRatio: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val hubRadius = outerRadius * 0.50f
            val packRadius = (outerRadius * tapePackRadiusRatio).coerceIn(hubRadius, outerRadius)

            // 1. Dark magnetic oxide tape roll around the reel hub
            drawCircle(
                color = Color(0xFF1F150F),
                radius = packRadius,
                center = center
            )
            // Fine concentric circles showing tightly wound tape layers
            if (packRadius > hubRadius + 2.dp.toPx()) {
                val layers = 5
                val step = (packRadius - hubRadius) / layers
                for (l in 1..layers) {
                    drawCircle(
                        color = if (l % 2 == 0) Color(0xFF2A1D15) else Color(0xFF170F0A),
                        radius = hubRadius + l * step,
                        center = center,
                        style = Stroke(0.8.dp.toPx())
                    )
                }
                // Sheen line across the tape roll
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF3E2C20).copy(alpha = 0.35f), Color.Transparent),
                        center = center - Offset(packRadius * 0.25f, packRadius * 0.25f),
                        radius = packRadius
                    ),
                    radius = packRadius,
                    center = center
                )
            }

            // 2. Outer White Hub Wheel (Wickelkern Kunststoff-Körper)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE4E9ED), Color(0xFFD2D8DE)),
                    center = center,
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF9AA4AE),
                radius = hubRadius,
                center = center,
                style = Stroke(1.dp.toPx())
            )
        }

        // Rotating inner teeth, spoke cutouts, and spindle drive hole
        Box(
            modifier = Modifier
                .fillMaxSize(0.52f)
                .rotate(rotation)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                val holeRadius = radius * 0.58f
                val toothTipRadius = holeRadius * 0.60f

                // 6 circular spoke cutouts in the white hub body
                val spokeDistance = (radius + holeRadius) / 2f
                val spokeHoleRadius = (radius - holeRadius) * 0.28f
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (PI / 180f).toFloat()
                    val spokeCenter = Offset(
                        center.x + spokeDistance * cos(angle),
                        center.y + spokeDistance * sin(angle)
                    )
                    drawCircle(Color(0xFFB0BAC4), spokeHoleRadius, spokeCenter)
                    drawCircle(Color.White.copy(alpha = 0.8f), spokeHoleRadius * 0.7f, spokeCenter - Offset(0.4f, 0.4f))
                }

                // Leader tape clamp pin (Klemmnadel) on the outer hub rim
                val clampAngle = 45f * (PI / 180f).toFloat()
                val clampCenter = Offset(
                    center.x + radius * 0.88f * cos(clampAngle),
                    center.y + radius * 0.88f * sin(clampAngle)
                )
                drawCircle(Color(0xFFE53935), radius * 0.08f, clampCenter)

                // Central spindle drive hole (deep dark chassis hole)
                drawCircle(Color(0xFF090A0D), holeRadius, center)

                // The 6 authentic drive teeth (Die 6 Zähnchen) pointing inwards!
                val teethCount = 6
                val baseAngleSpan = (11f * (PI / 180f)).toFloat()
                val tipAngleSpan = (7f * (PI / 180f)).toFloat()

                for (i in 0 until teethCount) {
                    val toothCenterAngle = (i * 60f) * (PI / 180f).toFloat()

                    val p1 = Offset(
                        center.x + holeRadius * cos(toothCenterAngle - baseAngleSpan),
                        center.y + holeRadius * sin(toothCenterAngle - baseAngleSpan)
                    )
                    val p2 = Offset(
                        center.x + toothTipRadius * cos(toothCenterAngle - tipAngleSpan),
                        center.y + toothTipRadius * sin(toothCenterAngle - tipAngleSpan)
                    )
                    val p3 = Offset(
                        center.x + toothTipRadius * cos(toothCenterAngle + tipAngleSpan),
                        center.y + toothTipRadius * sin(toothCenterAngle + tipAngleSpan)
                    )
                    val p4 = Offset(
                        center.x + holeRadius * cos(toothCenterAngle + baseAngleSpan),
                        center.y + holeRadius * sin(toothCenterAngle + baseAngleSpan)
                    )

                    val toothPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }

                    // Fill tooth with white plastic
                    drawPath(toothPath, Color(0xFFDCE2E7))
                    // Highlight on leading edge
                    drawLine(Color.White.copy(alpha = 0.8f), p1, p2, strokeWidth = 1.2f)
                    // Shadow on trailing edge
                    drawLine(Color(0xFF909AA4), p3, p4, strokeWidth = 1.2f)
                    // Tip line
                    drawLine(Color(0xFFBCC5CD), p2, p3, strokeWidth = 1.2f)
                }

                // Inner drive shaft clearance opening
                drawCircle(Color(0xFF090A0D), toothTipRadius * 0.85f, center)
            }
        }
    }
}

/**
 * Draws an authentic miniature metallic crosshead screw with recessed countersink bevel.
 */
private fun DrawScope.drawCassetteScrew(center: Offset, radius: Float, angleDeg: Float) {
    // Recessed countersink pocket
    drawCircle(Color.Black.copy(alpha = 0.5f), radius * 1.35f, center)
    drawCircle(Color.White.copy(alpha = 0.14f), radius * 1.35f, center, style = Stroke(0.8.dp.toPx()))

    // Polished metallic screw head
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE8ECF0), Color(0xFF9AA2AC), Color(0xFF6B7280)),
            start = center - Offset(radius, radius),
            end = center + Offset(radius, radius)
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = radius * 0.7f,
        center = center - Offset(radius * 0.2f, radius * 0.2f)
    )

    // Crosshead (Phillips '+') slot rotated by angleDeg
    val rad = angleDeg * (PI / 180f).toFloat()
    val slotLen = radius * 0.72f
    val cosA = cos(rad)
    val sinA = sin(rad)
    val slotColor = Color(0xFF1A1D24)
    val slotWidth = 1.3.dp.toPx()

    // Primary slot line
    drawLine(
        color = slotColor,
        start = Offset(center.x - slotLen * cosA, center.y - slotLen * sinA),
        end = Offset(center.x + slotLen * cosA, center.y + slotLen * sinA),
        strokeWidth = slotWidth
    )
    // Perpendicular slot line
    drawLine(
        color = slotColor,
        start = Offset(center.x + slotLen * sinA, center.y - slotLen * cosA),
        end = Offset(center.x - slotLen * sinA, center.y + slotLen * cosA),
        strokeWidth = slotWidth
    )
}
