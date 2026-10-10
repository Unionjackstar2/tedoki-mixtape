package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Cassette
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily

/**
 * Renders the spine (Rückseite / Schmalflanke) of a compact cassette inside its jewel case,
 * matching the authentic MC Kassettenregal look from the reference image:
 * - Acrylic clear plastic casing reflection
 * - Colorful paper spine insert (J-Card spine)
 * - Decorative stars/badges, centered handwritten/retro title, C-60 duration mark
 */
@Composable
fun CassetteSpineView(
    cassette: Cassette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spineColor = try {
        Color(android.graphics.Color.parseColor(cassette.colorHex))
    } catch (e: Exception) {
        Color(0xFF212328)
    }

    // Determine readable text color based on spine brightness
    val isLight = cassette.colorHex.equals("#DCD2BE", ignoreCase = true) ||
            cassette.colorHex.equals("#F7F2E6", ignoreCase = true)
    val spineTextColor = if (isLight) Color(0xFF183B7A) else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            // Acrylic outer jewel case gloss
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A4F58),
                        Color(0xFF1B1D22),
                        Color(0xFF2C3038)
                    )
                )
            )
            .border(1.dp, Color(0xFF6B7280).copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.5.dp)
    ) {
        // Colored Paper J-Card Spine Insert
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            spineColor.copy(alpha = 0.95f),
                            spineColor,
                            spineColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Plastic Case Sheen Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(0f, 2f),
                    end = Offset(size.width, 2f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.35f),
                    start = Offset(0f, size.height - 2f),
                    end = Offset(size.width, size.height - 2f),
                    strokeWidth = 1.5f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left badge / decorative symbol: e.g. ★ or [A]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "★",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color(0xFFE65100) else Color(0xFFFFB300)
                    )

                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isLight) Color(0xFF183B7A) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLight) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Centered Cassette Title (Handwritten Caveat font or bold retro style)
                Text(
                    text = cassette.title.ifBlank { "Mein Mixtape" },
                    fontFamily = CaveatFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = spineTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Right Badge: "C-60" or "60" duration in box (like the "90" in reference photo)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1A1C20))
                            .border(0.5.dp, Color(0xFF5A606D), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "60",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }

                    Text(
                        text = "★",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color(0xFFE65100) else Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}
