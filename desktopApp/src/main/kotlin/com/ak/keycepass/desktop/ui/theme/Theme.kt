package com.ak.keycepass.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// Palette KeycePass — "Nature Morte"
// Inspiration : natures mortes classiques (Chardin, Cézanne)
// Brun profond, olive, crème — couleurs posées, professionnelles
// Palette volontairement limitée pour un rendu sophistiqué
// ============================================================

// ── Status (muted, élégants) ──
val GreenPresent   = Color(0xFF4A7C59)   // Forest green
val GreenPresentBg = Color(0xFFE8F0E8)   // Vert pâle chaud
val YellowLate     = Color(0xFFB8860B)   // Dark goldenrod
val YellowLateBg   = Color(0xFFF5EDD6)   // Jaune pâle
val RedAbsent      = Color(0xFF8B3A3A)   // Brick red
val RedAbsentBg    = Color(0xFFF5E0E0)   // Rouge pâle
val BlueInfo       = Color(0xFF5B7B9A)   // Steel blue doux
val BlueInfoBg     = Color(0xFFE0E8F0)
val StatusPending  = Color(0xFF787878)   // Warm grey

// ── Surfaces (Light) ──
val LightSurface1   = Color(0xFFF7F3ED)   // Crème chaud
val LightSurface2   = Color(0xFFEDE4D4)   // Beige clair
val LightSurface3   = Color(0xFFD8CFC0)   // Beige moyen
val LightOnBg       = Color(0xFF2C2C2C)   // Charcoal
val LightOnBgVar    = Color(0xFF5C5C5C)   // Gris chaud

// ── Surfaces (Dark) ──
val DarkSurface1    = Color(0xFF1E1E1E)   // Deep charcoal
val DarkSurface2    = Color(0xFF2C2C2C)
val DarkSurface3    = Color(0xFF383838)
val DarkOnBg        = Color(0xFFE0DCD4)
val DarkOnBgVar     = Color(0xFFA89888)

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF5D4037),       // Brown 700 — ancre
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFEDE4D4),
    onPrimaryContainer = Color(0xFF3E2723),

    secondary          = Color(0xFF6B8E5E),       // Olive/Sage
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFE8F0E4),
    onSecondaryContainer = Color(0xFF2C4228),

    tertiary           = Color(0xFF8B7355),       // Warm taupe
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFFF0EBE0),
    onTertiaryContainer = Color(0xFF3E3525),

    error              = Color(0xFF8B3A3A),       // Brick
    onError            = Color.White,
    errorContainer     = Color(0xFFF5E0E0),
    onErrorContainer   = Color(0xFF451A1A),

    background         = LightSurface1,
    onBackground       = LightOnBg,
    surface            = Color.White,
    onSurface          = LightOnBg,
    surfaceVariant     = LightSurface2,
    onSurfaceVariant   = LightOnBgVar,
    outline            = Color(0xFFD8CFC0),
    outlineVariant     = Color(0xFFEDE4D4)
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFA67C6E),       // Brown clair
    onPrimary          = Color(0xFF3E2723),
    primaryContainer   = Color(0xFF4A3328),
    onPrimaryContainer = Color(0xFFEDE4D4),

    secondary          = Color(0xFF8FB089),       // Sage clair
    onSecondary        = Color(0xFF1A2E18),
    secondaryContainer = Color(0xFF2C4228),
    onSecondaryContainer = Color(0xFFE8F0E4),

    tertiary           = Color(0xFFBFAA8F),       // Taupe clair
    onTertiary         = Color(0xFF2C2418),
    tertiaryContainer  = Color(0xFF4A3A28),
    onTertiaryContainer = Color(0xFFF0EBE0),

    error              = Color(0xFFC17070),       // Brick clair
    onError            = Color(0xFF2C1010),
    errorContainer     = Color(0xFF5C2020),
    onErrorContainer   = Color(0xFFF5E0E0),

    background         = DarkSurface1,
    onBackground       = DarkOnBg,
    surface            = DarkSurface2,
    onSurface          = DarkOnBg,
    surfaceVariant     = DarkSurface3,
    onSurfaceVariant   = DarkOnBgVar,
    outline            = Color(0xFF505050),
    outlineVariant     = Color(0xFF383838)
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
