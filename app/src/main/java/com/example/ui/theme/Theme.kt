package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IceCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF004D5C),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = IceBlue,
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004C71),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = IceTeal,
    background = IceDeepNavy,
    onBackground = IceTextPrimary,
    surface = IceCardDark,
    onSurface = IceTextPrimary,
    surfaceVariant = Color(0xFF162536),
    onSurfaceVariant = IceTextSecondary,
    outline = IceCardStroke,
    error = IceRed
)

private val LightColorScheme = lightColorScheme(
    primary = IceCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E7FF),
    onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,
    tertiary = IceTeal,
    background = IceBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = IceSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = IceCardStrokeLight,
    error = IceRed
)

@Composable
fun ICEPOWERTheme(
    darkTheme: Boolean = true, // Default to futuristic icy dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

