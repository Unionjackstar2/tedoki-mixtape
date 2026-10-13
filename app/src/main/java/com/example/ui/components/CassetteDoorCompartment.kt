package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Cassette
import com.example.ui.theme.DeckAnthracite
import com.example.ui.theme.DeckChassisBevel
import com.example.ui.theme.DeckChassisHighlight
import com.example.ui.theme.ShareTechMonoFontFamily

/**
 * Authentic Vintage Cassette Deck Door (Kassettendeckel mit Acrylglas-Sichtfenster).
 *
 * Provides a photorealistic cassette compartment lid over the cassette well:
 * - Recessed dark interior chamber (Kassettenfach) with warm interior backlighting
 * - The cassette is housed inside the bay, with rotating reels and dynamic tape spools
 *   fully visible through the acrylic viewing window
 * - Robust brushed anthracite & metallic door frame with beveled edges and corner screws
 * - Clean vintage door frame without unwanted silkscreening or button labels
 * - Lower door crossbar with tactile finger grip grooves ("||||||||")
 * - Clear/smoked acrylic viewing window with:
 *     * Chrome/metallic inner bezel rim
 *     * Realistic diagonal glossy specular reflection sheen
 *     * Precision tape run gauge markings printed on the glass ("100 • • • 50 • • • 0")
 *     * Twin etched reel guide rings aligned right above the reel hubs
 *     * Inset shadows providing true mechanical depth behind the glass
 * - Soft door opening tilt and vertical eject animation when ejected
 */
@Composable
fun CassetteDoorCompartment(
    cassette: Cassette,
    isSpinning: Boolean,
    isRewinding: Boolean,
    isFastForwarding: Boolean,
    isRecording: Boolean,
    isEjected: Boolean,
    tapePositionSec: Int,
    totalCapacitySec: Int,
    onLabelClick: () -> Unit,
    onOpenCustomizer: (Cassette) -> Unit,
    onOpenDetail: ((Cassette) -> Unit)?,
    onInsertTape: () -> Unit,
    isSilverStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Smooth eject animation for door tilt and cassette lift
    val doorTiltAngle by animateFloatAsState(
        targetValue = if (isEjected) 18f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "doorTilt"
    )
    val cassetteEjectOffsetY by animateDpAsState(
        targetValue = if (isEjected) (-16).dp else 0.dp,
        animationSpec = tween(durationMillis = 420),
        label = "cassetteEjectY"
    )
    val doorOpenAlpha by animateFloatAsState(
        targetValue = if (isEjected) 0.65f else 1.0f,
        animationSpec = tween(durationMillis = 350),
        label = "doorAlpha"
    )

    // Outer Cassette Well Housing (Recessed into the deck chassis with 3D bevels)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isSilverStyle) {
                        listOf(Color(0xFFE2E7F0), Color(0xFFC8D0DE), Color(0xFFAAB3C2))
                    } else {
                        listOf(Color(0xFF1A1D24), Color(0xFF0E1014), Color(0xFF07080A))
                    }
                )
            )
            .border(
                2.dp,
                Brush.verticalGradient(
                    colors = if (isSilverStyle) {
                        listOf(Color(0xFFFFFFFF), Color(0xFFCBD2DE), Color(0xFF7E8896))
                    } else {
                        listOf(Color(0xFF4F5666), Color(0xFF1D2028))
                    }
                ),
                RoundedCornerShape(8.dp)
            )
            .padding(2.5.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF08090C))
        ) {
            val wellWidth = maxWidth
            // Base aspect ratio of the cassette compartment matching authentic cassette form factor
            val compartmentHeight = wellWidth * 0.615f

            // 1. Interior Chamber Backplate & Internal Illumination (Kassettenfachbeleuchtung)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compartmentHeight)
            ) {
                val w = size.width
                val h = size.height

                // Deep recessed well gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060709),
                            Color(0xFF0F1116),
                            Color(0xFF08090C)
                        )
                    )
                )

                // Warm compartment backlighting glow from upper chamber
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x35FFA000),
                            Color(0x18FF8F00),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.35f),
                        radius = w * 0.55f
                    ),
                    topLeft = Offset(w * 0.05f, 0f),
                    size = Size(w * 0.9f, h * 0.8f)
                )

                // Mechanical drive spindle shafts and guide notches in the backplate
                val hubY = h * 0.50f
                val leftHubX = w * 0.30f
                val rightHubX = w * 0.70f
                val spindleRadius = 14.dp.toPx()

                // Spindle recesses
                drawCircle(Color(0xFF050608), spindleRadius * 1.5f, Offset(leftHubX, hubY))
                drawCircle(Color(0xFF14171E), spindleRadius * 1.5f, Offset(leftHubX, hubY), style = Stroke(1.5.dp.toPx()))
                drawCircle(Color(0xFF050608), spindleRadius * 1.5f, Offset(rightHubX, hubY))
                drawCircle(Color(0xFF14171E), spindleRadius * 1.5f, Offset(rightHubX, hubY), style = Stroke(1.5.dp.toPx()))

                // Spindle metal drive pins (Kapstan / Spindelachse)
                drawCircle(Color(0xFF3A404D), spindleRadius * 0.45f, Offset(leftHubX, hubY))
                drawCircle(Color(0xFF6B7280), spindleRadius * 0.25f, Offset(leftHubX - 1f, hubY - 1f))
                drawCircle(Color(0xFF3A404D), spindleRadius * 0.45f, Offset(rightHubX, hubY))
                drawCircle(Color(0xFF6B7280), spindleRadius * 0.25f, Offset(rightHubX - 1f, hubY - 1f))

                // Guide rails / well frame inner shadows
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        startY = 0f,
                        endY = 16.dp.toPx()
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                        startX = 0f,
                        endX = 14.dp.toPx()
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startX = w - 14.dp.toPx(),
                        endX = w
                    )
                )
            }

            // 2. The Inserted Cassette (positioned in the chamber, spins in real time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compartmentHeight)
                    .clipToBounds()
                    .offset(y = cassetteEjectOffsetY),
                contentAlignment = Alignment.Center
            ) {
                CassetteView(
                    cassette = cassette,
                    isSpinning = isSpinning,
                    isRewinding = isRewinding,
                    isFastForwarding = isFastForwarding,
                    tapePositionSec = tapePositionSec,
                    totalCapacitySec = totalCapacitySec,
                    modifier = Modifier.fillMaxWidth(0.93f),
                    onLabelClick = onLabelClick
                )
            }

            // 3. The Cassette Deck Door (Kassettendeckel) with Acrylic Viewing Window (Sichtfenster)
            // Sits over the well, framing the cassette beautifully!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compartmentHeight)
                    .graphicsLayer {
                        // Realistic door tilt on bottom hinges when ejected
                        rotationX = doorTiltAngle
                        cameraDistance = 12f * density
                        alpha = doorOpenAlpha
                    }
            ) {
                // Outer Door Frame & Acrylic Viewing Window Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // --- A. DOOR PERIMETER & BEZEL ---
                    val frameCorner = 6.dp.toPx()

                    // Top Door Header Bar
                    val topBarH = h * 0.08f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSilverStyle) {
                                listOf(Color(0xFFF0F4F8), Color(0xFFD6DCE6), Color(0xFFBAC3D1))
                            } else {
                                listOf(Color(0xFF2E323A), Color(0xFF1B1D22), Color(0xFF15171B))
                            }
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, topBarH),
                        cornerRadius = CornerRadius(frameCorner, frameCorner)
                    )
                    // Top highlight bevel line
                    drawLine(
                        color = if (isSilverStyle) Color(0xFFFFFFFF) else Color(0xFF4F5666),
                        start = Offset(2.dp.toPx(), 1.dp.toPx()),
                        end = Offset(w - 2.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    // Bottom Door Crossbar (Heavy brushed metal door base)
                    val bottomBarH = h * 0.12f
                    val bottomBarY = h - bottomBarH
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSilverStyle) {
                                listOf(Color(0xFFE4E8F0), Color(0xFFCAD1DE), Color(0xFFAAB4C4))
                            } else {
                                listOf(Color(0xFF1A1C20), Color(0xFF262930), Color(0xFF141619))
                            }
                        ),
                        topLeft = Offset(0f, bottomBarY),
                        size = Size(w, bottomBarH),
                        cornerRadius = CornerRadius(frameCorner, frameCorner)
                    )
                    // Bottom bar top chamfer line
                    drawLine(
                        color = if (isSilverStyle) Color(0xFFFFFFFF) else Color(0xFF3E4452),
                        start = Offset(0f, bottomBarY),
                        end = Offset(w, bottomBarY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = if (isSilverStyle) Color(0xFF8B94A4) else Color(0xFF0A0C0E),
                        start = Offset(0f, bottomBarY + 1.5.dp.toPx()),
                        end = Offset(w, bottomBarY + 1.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Left & Right Door Frame Pillars
                    val sideBarW = w * 0.055f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = if (isSilverStyle) {
                                listOf(Color(0xFFD6DBE5), Color(0xFFC0C7D5))
                            } else {
                                listOf(Color(0xFF2B2F38), Color(0xFF181A1F))
                            }
                        ),
                        topLeft = Offset(0f, topBarH),
                        size = Size(sideBarW, bottomBarY - topBarH)
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = if (isSilverStyle) {
                                listOf(Color(0xFFC0C7D5), Color(0xFFD6DBE5))
                            } else {
                                listOf(Color(0xFF181A1F), Color(0xFF2B2F38))
                            }
                        ),
                        topLeft = Offset(w - sideBarW, topBarH),
                        size = Size(sideBarW, bottomBarY - topBarH)
                    )

                    // --- B. CENTRAL ACRYLIC VIEWING WINDOW (SICHTFENSTER) ---
                    val winX = sideBarW
                    val winY = topBarH
                    val winW = w - sideBarW * 2f
                    val winH = bottomBarY - topBarH
                    val winCorner = 5.dp.toPx()

                    // Smoked transparent acrylic tint over the viewing area
                    drawRoundRect(
                        color = Color(0x1A090C12),
                        topLeft = Offset(winX, winY),
                        size = Size(winW, winH),
                        cornerRadius = CornerRadius(winCorner, winCorner)
                    )

                    // Metallic Chrome Bevel Frame around the window cutout
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF5A6273),
                                Color(0xFF242730),
                                Color(0xFF454C5A)
                            ),
                            startY = winY,
                            endY = winY + winH
                        ),
                        topLeft = Offset(winX, winY),
                        size = Size(winW, winH),
                        cornerRadius = CornerRadius(winCorner, winCorner),
                        style = Stroke(1.8.dp.toPx())
                    )

                    // Inset shadow inside the window aperture (creating recessed cassette depth)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            startY = winY,
                            endY = winY + 10.dp.toPx()
                        ),
                        topLeft = Offset(winX, winY),
                        size = Size(winW, 10.dp.toPx())
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            startY = winY + winH - 8.dp.toPx(),
                            endY = winY + winH
                        ),
                        topLeft = Offset(winX, winY + winH - 8.dp.toPx()),
                        size = Size(winW, 8.dp.toPx())
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent),
                            startX = winX,
                            endX = winX + 8.dp.toPx()
                        ),
                        topLeft = Offset(winX, winY),
                        size = Size(8.dp.toPx(), winH)
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.50f)),
                            startX = winX + winW - 8.dp.toPx(),
                            endX = winX + winW
                        ),
                        topLeft = Offset(winX + winW - 8.dp.toPx(), winY),
                        size = Size(8.dp.toPx(), winH)
                    )

                    // --- C. REALISTIC GLASS SHEEN & DIAGONAL GLARE REFLECTION ---
                    // Main diagonal light glare across the acrylic glass pane
                    val glarePath = Path().apply {
                        moveTo(winX + winW * 0.15f, winY)
                        lineTo(winX + winW * 0.48f, winY)
                        lineTo(winX + winW * 0.22f, winY + winH)
                        lineTo(winX + winW * 0.02f, winY + winH)
                        close()
                    }
                    drawPath(
                        path = glarePath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            start = Offset(winX, winY),
                            end = Offset(winX + winW * 0.5f, winY + winH)
                        )
                    )

                    // Secondary soft ambient glass reflection stripe
                    val glarePath2 = Path().apply {
                        moveTo(winX + winW * 0.58f, winY)
                        lineTo(winX + winW * 0.74f, winY)
                        lineTo(winX + winW * 0.50f, winY + winH)
                        lineTo(winX + winW * 0.38f, winY + winH)
                        close()
                    }
                    drawPath(
                        path = glarePath2,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            start = Offset(winX + winW * 0.4f, winY),
                            end = Offset(winX + winW * 0.8f, winY + winH)
                        )
                    )

                    // Horizontal specular reflection catch along top edge of acrylic
                    drawLine(
                        color = Color.White.copy(alpha = 0.30f),
                        start = Offset(winX + 4.dp.toPx(), winY + 1.dp.toPx()),
                        end = Offset(winX + winW - 4.dp.toPx(), winY + 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // --- D. PRINTED SCALE / GAUGE ON THE ACRYLIC GLASS ---
                    // Two etched reel alignment guide rings printed on the glass
                    val hubLeftX = w * 0.30f
                    val hubRightX = w * 0.70f
                    val hubCenterY = h * 0.50f
                    val ringRadius = winH * 0.29f

                    // Left reel guide ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = ringRadius,
                        center = Offset(hubLeftX, hubCenterY),
                        style = Stroke(1.dp.toPx())
                    )
                    // Right reel guide ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = ringRadius,
                        center = Offset(hubRightX, hubCenterY),
                        style = Stroke(1.dp.toPx())
                    )

                    // Horizontal tape run indicator gauge lines printed on glass (e.g. 100 - 50 - 0)
                    val gaugeY = winY + winH * 0.79f
                    val gaugeStartX = winX + winW * 0.30f
                    val gaugeEndX = winX + winW * 0.70f
                    val gaugeLen = gaugeEndX - gaugeStartX
                    val tickCount = 9

                    for (i in 0 until tickCount) {
                        val tx = gaugeStartX + i * (gaugeLen / (tickCount - 1))
                        val isMajor = (i == 0 || i == 4 || i == tickCount - 1)
                        val tickLen = if (isMajor) 4.5.dp.toPx() else 2.5.dp.toPx()
                        val tickAlpha = if (isMajor) 0.32f else 0.18f
                        drawLine(
                            color = Color.White.copy(alpha = tickAlpha),
                            start = Offset(tx, gaugeY - tickLen / 2),
                            end = Offset(tx, gaugeY + tickLen / 2),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // --- E. BOTTOM BAR TACTILE GRIP GROOVES & HARDWARE DETAILS ---
                    // Finger grip ridges in the center of the bottom door bar
                    val gripW = w * 0.30f
                    val gripStartX = (w - gripW) / 2f
                    val gripCount = 14
                    val gripStep = gripW / gripCount
                    val gripY1 = bottomBarY + bottomBarH * 0.30f
                    val gripY2 = bottomBarY + bottomBarH * 0.70f

                    for (i in 0 until gripCount) {
                        val gx = gripStartX + i * gripStep
                        // Dark groove
                        drawLine(
                            color = Color(0xFF0E1013),
                            start = Offset(gx, gripY1),
                            end = Offset(gx, gripY2),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        // Metallic highlight edge
                        drawLine(
                            color = Color(0xFF3E4554),
                            start = Offset(gx + 1.2.dp.toPx(), gripY1),
                            end = Offset(gx + 1.2.dp.toPx(), gripY2),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Subtle corner hex screws on the 4 corners of the door
                    val screwR = 2.5.dp.toPx()
                    val screwInsetX = sideBarW * 0.45f
                    val screwInsetTopY = topBarH * 0.50f
                    val screwInsetBottomY = bottomBarY + bottomBarH * 0.50f

                    listOf(
                        Offset(screwInsetX, screwInsetTopY),
                        Offset(w - screwInsetX, screwInsetTopY),
                        Offset(screwInsetX, screwInsetBottomY),
                        Offset(w - screwInsetX, screwInsetBottomY)
                    ).forEach { pt ->
                        drawCircle(Color(0xFF131518), screwR * 1.3f, pt)
                        drawCircle(Color(0xFF555C6B), screwR, pt)
                        drawLine(
                            color = Color(0xFF24272E),
                            start = Offset(pt.x - screwR * 0.7f, pt.y),
                            end = Offset(pt.x + screwR * 0.7f, pt.y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Click interceptor to open label customizer through the window
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isEjected) {
                                onLabelClick()
                            }
                        }
                )
            }

            // 4. Eject Action Overlay (when cassette is ejected)
            if (isEjected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Kassette ausgeworfen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenCustomizer(cassette) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B55))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Beschriften")
                            }

                            if (onOpenDetail != null) {
                                Button(
                                    onClick = { onOpenDetail(cassette) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Datei-Info")
                                }
                            }

                            Button(
                                onClick = onInsertTape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                            ) {
                                Text("Einlegen", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
