package com.mytube.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val BrandCoral = Color(0xFFFF4757)
val BrandPink = Color(0xFFFF2D78)

private val DarkColorScheme = darkColorScheme(
    primary = BrandCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF54172A),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = BrandPink,
    onSecondary = Color.White,
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFF2F0F5),
    surface = Color(0xFF17171F),
    onSurface = Color(0xFFF2F0F5),
    surfaceVariant = Color(0xFF242430),
    onSurfaceVariant = Color(0xFFC9C5D0),
    outline = Color(0xFF44414E),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD91C55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E0019),
    secondary = Color(0xFFB81763),
    onSecondary = Color.White,
    background = Color(0xFFF8F7FA),
    onBackground = Color(0xFF1D1B20),
    surface = Color.White,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF0EDF3),
    onSurfaceVariant = Color(0xFF625D68),
    outline = Color(0xFF827A86),
)

private val MyTubeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun MyTubeTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
        shapes = MyTubeShapes,
        content = content,
    )
}
