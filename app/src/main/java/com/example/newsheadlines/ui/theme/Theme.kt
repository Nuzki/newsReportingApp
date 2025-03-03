package com.example.newsheadlines.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xFF64B5F6),
    primaryVariant = Color(0xFF42A5F5),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF1976D2),
    primaryVariant = Color(0xFF0288D1),
    secondary = Color(0xFF4FC3F7),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
)

@Composable
fun NewsHeadlinesTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}