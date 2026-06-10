package com.ak.keycepass.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ===== Palette KeycePass — Modern Education Theme =====
// Bleu profond professionnel + accents vibrants

// Status colors
val GreenPresent = Color(0xFF10B981)
val GreenPresentBg = Color(0xFFD1FAE5)
val YellowLate = Color(0xFFF59E0B)
val YellowLateBg = Color(0xFFFEF3C7)
val RedAbsent = Color(0xFFEF4444)
val RedAbsentBg = Color(0xFFFEE2E2)
val BlueInfo = Color(0xFF3B82F6)
val BlueInfoBg = Color(0xFFDBEAFE)
val PurpleAccent = Color(0xFF8B5CF6)
val PurpleAccentBg = Color(0xFFEDE9FE)

// Gradients
val GradientBlue = listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
val GradientGreen = listOf(Color(0xFF059669), Color(0xFF10B981))
val GradientOrange = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
val GradientRed = listOf(Color(0xFFDC2626), Color(0xFFEF4444))
val GradientPurple = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6))

// Surface colors for dark theme (elegant glassmorphism)
val DarkSurface1 = Color(0xFF1E1E2E)
val DarkSurface2 = Color(0xFF2A2A3E)
val DarkSurface3 = Color(0xFF363650)
val LightSurface1 = Color(0xFFF8FAFC)
val LightSurface2 = Color(0xFFF1F5F9)
val LightSurface3 = Color(0xFFE2E8F0)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),          // Blue-600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), // Blue-100
    onPrimaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFF7C3AED),        // Violet-600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = Color(0xFF059669),         // Emerald-600
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF022C22),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = LightSurface1,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurface2,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),          // Blue-400
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA78BFA),        // Violet-400
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF34D399),         // Emerald-400
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF022C22),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color(0xFF0F172A),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkSurface1,
    onBackground = Color(0xFFE2E8F0),
    surface = DarkSurface2,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = DarkSurface3,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
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
