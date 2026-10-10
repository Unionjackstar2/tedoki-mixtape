package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.RadioStation

/**
 * Renders the radio station logo if a favicon/logo URL is available,
 * otherwise falls back to the distinctive retro color pill badge.
 */
@Composable
fun StationLogoView(
    station: RadioStation?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    fallbackTextSize: TextUnit = 11.sp
) {
    val logoBg = try {
        Color(android.graphics.Color.parseColor(station?.logoBgHex ?: "#E91E63"))
    } catch (e: Exception) {
        Color(0xFFE91E63)
    }

    val faviconUrl = station?.favicon?.trim()
    var hasError by remember(faviconUrl) { mutableStateOf(false) }

    if (!faviconUrl.isNullOrBlank() && !hasError) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF1E2127))
                .border(0.8.dp, Color(0xFF383D47), RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(faviconUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 TedokiMixtape/1.0")
                    .crossfade(true)
                    .build(),
                contentDescription = station?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                onError = {
                    hasError = true
                }
            )
        }
    } else {
        // Fallback: Retro colored badge pill
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(logoBg)
                .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = station?.logoText?.ifBlank { station.name.take(4) } ?: "RADIO",
                fontSize = fallbackTextSize,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}
