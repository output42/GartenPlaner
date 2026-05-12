package de.gartenplaner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = SurfaceLight,
    primaryContainer = Color(0xFFD8EED8),
    background       = BackgroundLight,
    surface          = SurfaceLight,
    surfaceVariant   = Surface2Light,
    onBackground     = TextLight,
    onSurface        = TextLight,
    outline          = BorderLight,
)

private val DarkColors = darkColorScheme(
    primary          = GreenAccent,
    onPrimary        = Color(0xFF091409),
    primaryContainer = Color(0xFF1E2E1E),
    background       = BackgroundDark,
    surface          = SurfaceDark,
    surfaceVariant   = Surface2Dark,
    onBackground     = TextDark,
    onSurface        = TextDark,
    outline          = BorderDark,
)

@Composable
fun GartenPlanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = GartenPlanerTypography,
        content     = content
    )
}
