package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RetroDarkColorScheme = darkColorScheme(
    primary = AmberScaleText,
    onPrimary = DeckSurfaceDark,
    secondary = LedRecRed,
    onSecondary = DeckSurfaceDark,
    tertiary = LedStereoGreen,
    background = DeckSurfaceDark,
    onBackground = DigitalLcdText,
    surface = DeckAnthracite,
    onSurface = DigitalLcdText,
    surfaceVariant = DeckChassisBevel,
    onSurfaceVariant = DigitalLcdSubtext
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = RetroDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeckSurfaceDark.toArgb()
                window.navigationBarColor = DeckSurfaceDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
