package de.tedoki.mixtape.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.R

// Handwritten font for authentic cassette labels
val CaveatFontFamily = FontFamily(
    Font(R.font.caveat, FontWeight.Normal),
    Font(R.font.caveat, FontWeight.Bold)
)

// Monospace font for odometer counter and digital tuner
val ShareTechMonoFontFamily = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

// Authentic retro 16x16 dot matrix font
val DotGothic16FontFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
