package com.ak.keycepass.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Couleurs personnalisées KeycePass
val GreenPresent = Color(0xFF4CAF50)
val YellowLate = Color(0xFFFFC107)
val RedAbsent = Color(0xFFF44336)
val BluePrimary = Color(0xFF1565C0)
val BlueSecondary = Color(0xFF42A5F5)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = GreenPresent,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EDF2),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    error = RedAbsent
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueSecondary,
    secondary = BluePrimary,
    tertiary = GreenPresent,
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    surfaceVariant = Color(0xFF0F3460),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8),
    error = RedAbsent
)

@Composable
fun KeycePassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
