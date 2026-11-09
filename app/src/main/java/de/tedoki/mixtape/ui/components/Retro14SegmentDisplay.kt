package de.tedoki.mixtape.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.ui.theme.DotGothic16FontFamily
import de.tedoki.mixtape.ui.theme.RetroOrangeDimmed
import de.tedoki.mixtape.ui.theme.RetroOrangeDisplayBg
import de.tedoki.mixtape.ui.theme.RetroOrangeGlow
import de.tedoki.mixtape.ui.theme.RetroOrangeLit
import de.tedoki.mixtape.ui.theme.RetroOrangeSubtext
import de.tedoki.mixtape.ui.theme.RetroOrangeSubtextDimmed
import de.tedoki.mixtape.ui.theme.RetroOrangeUnlit
import de.tedoki.mixtape.ui.theme.ShareTechMonoFontFamily
import kotlin.math.min

enum class RetroDisplayMode {
    FOURTEEN_SEGMENT,
    DOT_MATRIX
}

/**
 * 14-Segment character font mapping for ASCII alphanumeric characters.
 *
 * Segment layout:
 *      -- A --
 *    |\   |   /|
 *    F H  J  K B
 *    |  \ | /  |
 *    -G1- + -G2-
 *    |  / | \  |
 *    E L  M  N C
 *    |/   |   \|
 *      -- D --
 */
object FourteenSegmentFont {
    const val A  = 1 shl 0  // Top horizontal
    const val B  = 1 shl 1  // Top right vertical
    const val C  = 1 shl 2  // Bottom right vertical
    const val D  = 1 shl 3  // Bottom horizontal
    const val E  = 1 shl 4  // Bottom left vertical
    const val F  = 1 shl 5  // Top left vertical
    const val G1 = 1 shl 6  // Middle left horizontal
    const val G2 = 1 shl 7  // Middle right horizontal
    const val H  = 1 shl 8  // Top left diagonal
    const val J  = 1 shl 9  // Top center vertical
    const val K  = 1 shl 10 // Top right diagonal
    const val L  = 1 shl 11 // Bottom left diagonal
    const val M  = 1 shl 12 // Bottom center vertical
    const val N  = 1 shl 13 // Bottom right diagonal

    fun getMask(char: Char): Int {
        return when (char.uppercaseChar()) {
            ' ' -> 0
            'A' -> A or B or C or E or F or G1 or G2
            'B' -> A or B or C or D or J or M or G2
            'C' -> A or D or E or F
            'D' -> A or B or C or D or J or M
            'E' -> A or D or E or F or G1 or G2
            'F' -> A or E or F or G1
            'G' -> A or C or D or E or F or G2
            'H' -> B or C or E or F or G1 or G2
            'I' -> A or D or J or M
            'J' -> B or C or D or E
            'K' -> E or F or G1 or K or N
            'L' -> D or E or F
            'M' -> B or C or E or F or H or K
            'N' -> B or C or E or F or H or N
            'O' -> A or B or C or D or E or F
            'P' -> A or B or E or F or G1 or G2
            'Q' -> A or B or C or D or E or F or N
            'R' -> A or B or E or F or G1 or G2 or N
            'S' -> A or C or D or F or G1 or G2
            'T' -> A or J or M
            'U' -> B or C or D or E or F
            'V' -> E or F or K or L
            'W' -> B or C or E or F or L or N
            'X' -> H or K or L or N
            'Y' -> H or K or M
            'Z' -> A or D or K or L
            '0' -> A or B or C or D or E or F or K or L
            '1' -> B or C
            '2' -> A or B or D or E or G1 or G2
            '3' -> A or B or C or D or G2
            '4' -> B or C or F or G1 or G2
            '5' -> A or C or D or F or G1 or G2
            '6' -> A or C or D or E or F or G1 or G2
            '7' -> A or B or C
            '8' -> A or B or C or D or E or F or G1 or G2
            '9' -> A or B or C or D or F or G1 or G2
            '-' -> G1 or G2
            '+' -> G1 or G2 or J or M
            '*' -> G1 or G2 or J or M or H or K or L or N
            '/' -> K or L
            '\\' -> H or N
            '_' -> D
            '=' -> D or G1 or G2
            '?' -> A or B or G2 or M
            '!' -> B or C
            '.' -> D
            ':' -> J or M
            else -> 0
        }
    }
}

/**
 * 5x7 Dot-Matrix font table for classic retro LED/VFD tuner displays.
 */
object DotMatrixFont {
    fun getCols(char: Char): IntArray {
        return when (char.uppercaseChar()) {
            ' ' -> intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
            'A' -> intArrayOf(0x7E, 0x11, 0x11, 0x11, 0x7E)
            'B' -> intArrayOf(0x7F, 0x49, 0x49, 0x49, 0x36)
            'C' -> intArrayOf(0x3E, 0x41, 0x41, 0x41, 0x22)
            'D' -> intArrayOf(0x7F, 0x41, 0x41, 0x22, 0x1C)
            'E' -> intArrayOf(0x7F, 0x49, 0x49, 0x49, 0x41)
            'F' -> intArrayOf(0x7F, 0x09, 0x09, 0x09, 0x01)
            'G' -> intArrayOf(0x3E, 0x41, 0x49, 0x49, 0x7A)
            'H' -> intArrayOf(0x7F, 0x08, 0x08, 0x08, 0x7F)
            'I' -> intArrayOf(0x00, 0x41, 0x7F, 0x41, 0x00)
            'J' -> intArrayOf(0x20, 0x40, 0x41, 0x3F, 0x01)
            'K' -> intArrayOf(0x7F, 0x08, 0x14, 0x22, 0x41)
            'L' -> intArrayOf(0x7F, 0x40, 0x40, 0x40, 0x40)
            'M' -> intArrayOf(0x7F, 0x02, 0x0C, 0x02, 0x7F)
            'N' -> intArrayOf(0x7F, 0x04, 0x08, 0x10, 0x7F)
            'O' -> intArrayOf(0x3E, 0x41, 0x41, 0x41, 0x3E)
            'P' -> intArrayOf(0x7F, 0x09, 0x09, 0x09, 0x06)
            'Q' -> intArrayOf(0x3E, 0x41, 0x51, 0x21, 0x5E)
            'R' -> intArrayOf(0x7F, 0x09, 0x19, 0x29, 0x46)
            'S' -> intArrayOf(0x46, 0x49, 0x49, 0x49, 0x31)
            'T' -> intArrayOf(0x01, 0x01, 0x7F, 0x01, 0x01)
            'U' -> intArrayOf(0x3F, 0x40, 0x40, 0x40, 0x3F)
            'V' -> intArrayOf(0x1F, 0x20, 0x40, 0x20, 0x1F)
            'W' -> intArrayOf(0x7F, 0x20, 0x18, 0x20, 0x7F)
            'X' -> intArrayOf(0x63, 0x14, 0x08, 0x14, 0x63)
            'Y' -> intArrayOf(0x07, 0x08, 0x70, 0x08, 0x07)
            'Z' -> intArrayOf(0x61, 0x51, 0x49, 0x45, 0x43)
            '0' -> intArrayOf(0x3E, 0x51, 0x49, 0x45, 0x3E)
            '1' -> intArrayOf(0x00, 0x42, 0x7F, 0x40, 0x00)
            '2' -> intArrayOf(0x42, 0x61, 0x51, 0x49, 0x46)
            '3' -> intArrayOf(0x21, 0x41, 0x45, 0x4B, 0x31)
            '4' -> intArrayOf(0x18, 0x14, 0x12, 0x7F, 0x10)
            '5' -> intArrayOf(0x27, 0x45, 0x45, 0x45, 0x39)
            '6' -> intArrayOf(0x3C, 0x4A, 0x49, 0x49, 0x30)
            '7' -> intArrayOf(0x01, 0x71, 0x09, 0x05, 0x03)
            '8' -> intArrayOf(0x36, 0x49, 0x49, 0x49, 0x36)
            '9' -> intArrayOf(0x06, 0x49, 0x49, 0x29, 0x1E)
            '-' -> intArrayOf(0x08, 0x08, 0x08, 0x08, 0x08)
            '.' -> intArrayOf(0x00, 0x60, 0x60, 0x00, 0x00)
            '/' -> intArrayOf(0x60, 0x18, 0x06, 0x01, 0x00)
            ':' -> intArrayOf(0x00, 0x36, 0x36, 0x00, 0x00)
            '+' -> intArrayOf(0x08, 0x08, 0x3E, 0x08, 0x08)
            '*' -> intArrayOf(0x14, 0x08, 0x3E, 0x08, 0x14)
            '!' -> intArrayOf(0x00, 0x00, 0x5F, 0x00, 0x00)
            '?' -> intArrayOf(0x02, 0x01, 0x51, 0x09, 0x06)
            else -> intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
        }
    }
}

/**
 * Sanitizes and formats text to fit exactly into a vintage 10-character display.
 * German umlauts are normalized to standard ASCII characters as done by vintage RDS tuners.
 * The text is truncated to at most 10 characters (right-cut off).
 */
fun sanitizeRetroText(raw: String): String {
    return raw.trim()
        .replace("ä", "A").replace("Ä", "A")
        .replace("ö", "O").replace("Ö", "O")
        .replace("ü", "U").replace("Ü", "U")
        .replace("ß", "S")
        .uppercase()
        .take(10)
}

/**
 * Vintage Hi-Fi 10-digit 14-Segment / Dot-Matrix Radio Display.
 * Renders exactly 10 character positions in warm vintage soft orange ("Sanftes Orange"),
 * with realistic faint unlit segment ghosts in the smoky dark acrylic bezel.
 */
@Composable
fun RetroRadioDisplay(
    stationName: String,
    subtext: String,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    initialMode: RetroDisplayMode = RetroDisplayMode.FOURTEEN_SEGMENT,
    onDisplayClick: () -> Unit = {}
) {
    var displayMode by remember { mutableStateOf(initialMode) }

    // Normalize and prepare exactly 10 character slots
    val cleanName = sanitizeRetroText(stationName)
    val characters10 = cleanName.padEnd(10, ' ').take(10)

    val litColor by animateColorAsState(
        targetValue = if (isDimmed) RetroOrangeDimmed else RetroOrangeLit,
        animationSpec = tween(durationMillis = 300),
        label = "retroLitColor"
    )

    val unlitColor = if (isDimmed) Color(0xFF180D04) else RetroOrangeUnlit
    val subtextColor = if (isDimmed) RetroOrangeSubtextDimmed else RetroOrangeSubtext

    // Recessed dark display glass window with vintage amber undertone and 3D bevels
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0904),
                        RetroOrangeDisplayBg,
                        Color(0xFF0A0703)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF5A3C1E), Color(0xFF221406))
                ),
                RoundedCornerShape(6.dp)
            )
            .clickable { onDisplayClick() }
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar inside display glass: RDS indicator & Mode switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RDS PS [10-CH]",
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDimmed) Color(0xFF4A321B) else Color(0xFF8C5C28),
                    letterSpacing = 0.5.sp
                )

                // Mode indicator & toggle chip: 14-SEG / MATRIX
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF140D05))
                        .border(0.5.dp, Color(0xFF33200E), RoundedCornerShape(3.dp))
                        .clickable {
                            displayMode = if (displayMode == RetroDisplayMode.FOURTEEN_SEGMENT) {
                                RetroDisplayMode.DOT_MATRIX
                            } else {
                                RetroDisplayMode.FOURTEEN_SEGMENT
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (displayMode == RetroDisplayMode.FOURTEEN_SEGMENT) "14-SEG" else "MATRIX",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDimmed) Color(0xFF5A3C1F) else RetroOrangeSubtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Main 10-digit display canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val totalWidth = size.width
                    val totalHeight = size.height

                    val charCount = 10
                    val gap = 2.dp.toPx()
                    val totalGaps = gap * (charCount - 1)
                    val charWidth = (totalWidth - totalGaps) / charCount

                    for (i in 0 until charCount) {
                        val charX = i * (charWidth + gap)
                        val ch = characters10[i]

                        when (displayMode) {
                            RetroDisplayMode.FOURTEEN_SEGMENT -> {
                                draw14SegmentChar(
                                    char = ch,
                                    x = charX,
                                    y = 0f,
                                    width = charWidth,
                                    height = totalHeight,
                                    litColor = litColor,
                                    unlitColor = unlitColor,
                                    isDimmed = isDimmed
                                )
                            }
                            RetroDisplayMode.DOT_MATRIX -> {
                                draw5x7MatrixChar(
                                    char = ch,
                                    x = charX,
                                    y = 0f,
                                    width = charWidth,
                                    height = totalHeight,
                                    litColor = litColor,
                                    unlitColor = unlitColor,
                                    isDimmed = isDimmed
                                )
                            }
                        }
                    }

                    // Realistic diagonal specular glass reflection streak across 10-digit display window
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x00FFFFFF),
                                Color(0x14FFFFFF),
                                Color(0x22FFFFFF),
                                Color(0x00FFFFFF)
                            ),
                            start = Offset(0f, totalHeight),
                            end = Offset(totalWidth * 0.7f, 0f)
                        ),
                        start = Offset(0f, totalHeight),
                        end = Offset(totalWidth * 0.7f, 0f),
                        strokeWidth = 14.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Subtext: Frequency / RDS Now-Playing / Slogan in matching vintage amber
            Text(
                text = subtext,
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 10.sp,
                color = subtextColor,
                maxLines = 1,
                lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Draws a single 14-segment character at (x, y) with dimensions (width, height).
 * Uses an italic tilt (~7°) and draws unlit segments faintly for authentic display realism.
 */
private fun DrawScope.draw14SegmentChar(
    char: Char,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    litColor: Color,
    unlitColor: Color,
    isDimmed: Boolean
) {
    val mask = FourteenSegmentFont.getMask(char)

    // Slant offset for classic italic display
    val slant = width * 0.12f
    fun xAt(rx: Float, ry: Float): Float = x + rx + slant * (1f - ry / height)
    fun yAt(ry: Float): Float = y + ry

    val t = min(width, height) * 0.12f // segment thickness
    val g = t * 0.35f                  // gap between segments

    val x0 = g * 2f
    val x1 = width - g * 2f
    val y0 = g * 2f
    val y1 = height - g * 2f
    val xm = width / 2f
    val ym = height / 2f

    // Helper to draw a segment polygon
    fun drawSegment(flag: Int, points: List<Pair<Float, Float>>) {
        val isLit = (mask and flag) != 0
        val color = if (isLit) litColor else unlitColor

        val path = Path()
        val first = points.first()
        path.moveTo(xAt(first.first, first.second), yAt(first.second))
        for (idx in 1 until points.size) {
            val p = points[idx]
            path.lineTo(xAt(p.first, p.second), yAt(p.second))
        }
        path.close()

        drawPath(path, color, style = Fill)

        // Subtle glow bloom for lit segments
        if (isLit && !isDimmed) {
            drawPath(path, RetroOrangeGlow, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }
    }

    // Helper to draw diagonal or straight bar
    fun drawBarSegment(flag: Int, p1: Pair<Float, Float>, p2: Pair<Float, Float>, strokeWidth: Float = t * 0.85f) {
        val isLit = (mask and flag) != 0
        val color = if (isLit) litColor else unlitColor

        val start = Offset(xAt(p1.first, p1.second), yAt(p1.second))
        val end = Offset(xAt(p2.first, p2.second), yAt(p2.second))

        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (isLit && !isDimmed) {
            drawLine(
                color = RetroOrangeGlow,
                start = start,
                end = end,
                strokeWidth = strokeWidth + 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 1. A (Top Horizontal)
    drawSegment(
        FourteenSegmentFont.A,
        listOf(
            Pair(x0 + t * 0.6f, y0),
            Pair(x1 - t * 0.6f, y0),
            Pair(x1 - t, y0 + t),
            Pair(x0 + t, y0 + t)
        )
    )

    // 2. D (Bottom Horizontal)
    drawSegment(
        FourteenSegmentFont.D,
        listOf(
            Pair(x0 + t, y1 - t),
            Pair(x1 - t, y1 - t),
            Pair(x1 - t * 0.6f, y1),
            Pair(x0 + t * 0.6f, y1)
        )
    )

    // 3. F (Top-Left Vertical)
    drawSegment(
        FourteenSegmentFont.F,
        listOf(
            Pair(x0, y0 + t * 0.6f),
            Pair(x0 + t, y0 + t),
            Pair(x0 + t, ym - g),
            Pair(x0, ym - g)
        )
    )

    // 4. B (Top-Right Vertical)
    drawSegment(
        FourteenSegmentFont.B,
        listOf(
            Pair(x1 - t, y0 + t),
            Pair(x1, y0 + t * 0.6f),
            Pair(x1, ym - g),
            Pair(x1 - t, ym - g)
        )
    )

    // 5. E (Bottom-Left Vertical)
    drawSegment(
        FourteenSegmentFont.E,
        listOf(
            Pair(x0, ym + g),
            Pair(x0 + t, ym + g),
            Pair(x0 + t, y1 - t),
            Pair(x0, y1 - t * 0.6f)
        )
    )

    // 6. C (Bottom-Right Vertical)
    drawSegment(
        FourteenSegmentFont.C,
        listOf(
            Pair(x1 - t, ym + g),
            Pair(x1, ym + g),
            Pair(x1, y1 - t * 0.6f),
            Pair(x1 - t, y1 - t)
        )
    )

    // 7. G1 (Middle-Left Horizontal)
    drawSegment(
        FourteenSegmentFont.G1,
        listOf(
            Pair(x0 + t + g, ym - t * 0.45f),
            Pair(xm - g, ym - t * 0.45f),
            Pair(xm - g, ym + t * 0.45f),
            Pair(x0 + t + g, ym + t * 0.45f)
        )
    )

    // 8. G2 (Middle-Right Horizontal)
    drawSegment(
        FourteenSegmentFont.G2,
        listOf(
            Pair(xm + g, ym - t * 0.45f),
            Pair(x1 - t - g, ym - t * 0.45f),
            Pair(x1 - t - g, ym + t * 0.45f),
            Pair(xm + g, ym + t * 0.45f)
        )
    )

    // 9. J (Top-Center Vertical)
    drawBarSegment(
        FourteenSegmentFont.J,
        Pair(xm, y0 + t + g),
        Pair(xm, ym - t * 0.5f - g)
    )

    // 10. M (Bottom-Center Vertical)
    drawBarSegment(
        FourteenSegmentFont.M,
        Pair(xm, ym + t * 0.5f + g),
        Pair(xm, y1 - t - g)
    )

    // 11. H (Top-Left Diagonal)
    drawBarSegment(
        FourteenSegmentFont.H,
        Pair(x0 + t + g, y0 + t + g),
        Pair(xm - t * 0.4f - g, ym - t * 0.4f - g)
    )

    // 12. K (Top-Right Diagonal)
    drawBarSegment(
        FourteenSegmentFont.K,
        Pair(x1 - t - g, y0 + t + g),
        Pair(xm + t * 0.4f + g, ym - t * 0.4f - g)
    )

    // 13. L (Bottom-Left Diagonal)
    drawBarSegment(
        FourteenSegmentFont.L,
        Pair(x0 + t + g, y1 - t - g),
        Pair(xm - t * 0.4f - g, ym + t * 0.4f + g)
    )

    // 14. N (Bottom-Right Diagonal)
    drawBarSegment(
        FourteenSegmentFont.N,
        Pair(x1 - t - g, y1 - t - g),
        Pair(xm + t * 0.4f + g, ym + t * 0.4f + g)
    )
}

/**
 * Draws a single 5x7 dot-matrix character at (x, y) with dimensions (width, height).
 */
private fun DrawScope.draw5x7MatrixChar(
    char: Char,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    litColor: Color,
    unlitColor: Color,
    isDimmed: Boolean
) {
    val cols = DotMatrixFont.getCols(char)
    val slant = width * 0.10f

    val pitchX = width / 5.5f
    val pitchY = height / 7.5f
    val radius = min(pitchX, pitchY) * 0.38f

    val startX = x + pitchX * 0.35f
    val startY = y + pitchY * 0.35f

    for (col in 0 until 5) {
        val colBits = cols[col]
        for (row in 0 until 7) {
            val isLit = ((colBits shr row) and 1) != 0
            val color = if (isLit) litColor else unlitColor

            val rowSlant = slant * (1f - row / 6f)
            val cx = startX + col * pitchX + rowSlant
            val cy = startY + row * pitchY

            drawCircle(
                color = color,
                radius = radius,
                center = Offset(cx, cy)
            )

            if (isLit && !isDimmed) {
                drawCircle(
                    color = RetroOrangeGlow,
                    radius = radius + 0.8.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
