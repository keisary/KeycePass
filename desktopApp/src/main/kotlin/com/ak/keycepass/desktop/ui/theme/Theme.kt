package com.ak.keycepass.desktop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// Palette KeycePass — "Monochrome"
// Inspiration : Linear, Stripe, Vercel — noir & blanc pur
// ZERO couleur inutile. Uniquement du noir, blanc, gris.
// Les seules couleurs tolerees : statuts ultra subtils.
// ============================================================

// ── Status (minimum, subtil) ──
val StatusPresent = Color(0xFF22C55E)    // Green-500 (seul point couleur)
val StatusPresentBg = Color(0xFF0A1A0A) // Vert quasi invisible
val StatusLate = Color(0xFFA3A3A3)      // Gris — ni jaune, ni flashy
val StatusLateBg = Color(0xFF1A1A1A)
val StatusAbsent = Color(0xFF6B3A3A)    // Rouge fonce, discret
val StatusAbsentBg = Color(0xFF1A0A0A)
val InfoBlue = Color(0xFF3B82F6)        // Accent tres leger (optionnel)
val StatusPending = Color(0xFF525252)

// ── Noir & Blanc (Light) ──
val White = Color(0xFFFFFFFF)
val LightBg = Color(0xFFF5F5F5)         // Off-white
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEEEEEE)
val LightSurface3 = Color(0xFFE0E0E0)
val LightOnBg = Color(0xFF0A0A0A)
val LightOnBgVar = Color(0xFF737373)
val LightBorder = Color(0xFFE0E0E0)
val LightBorderFocus = Color(0xFF0A0A0A)

// ── Noir & Blanc (Dark) ──
val DarkBg = Color(0xFF000000)          // PURE BLACK
val DarkSurface = Color(0xFF0A0A0A)     // Almost black
val DarkSurface2 = Color(0xFF141414)
val DarkSurface3 = Color(0xFF1A1A1A)
val DarkOnBg = Color(0xFFF5F5F5)        // Presque blanc
val DarkOnBgVar = Color(0xFF737373)
val DarkBorder = Color(0xFF1A1A1A)
val DarkBorderFocus = Color(0xFFF5F5F5)

// ── Light Scheme ──

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF0A0A0A),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFEEEEEE),
    onPrimaryContainer = Color(0xFF0A0A0A),

    secondary          = Color(0xFF525252),
    onSecondary        = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF0A0A0A),

    tertiary           = Color(0xFF737373),
    onTertiary         = Color(0xFFFFFFFF),
    tertiaryContainer  = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF0A0A0A),

    error              = Color(0xFF6B3A3A),
    onError            = Color(0xFFFFFFFF),
    errorContainer     = Color(0xFFF5E0E0),
    onErrorContainer   = Color(0xFF2C1010),

    background         = LightBg,
    onBackground       = LightOnBg,
    surface            = LightSurface,
    onSurface          = LightOnBg,
    surfaceVariant     = LightSurface2,
    onSurfaceVariant   = LightOnBgVar,
    outline            = LightBorder,
    outlineVariant     = LightSurface3
)

// ── Dark Scheme ──

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFF5F5F5),
    onPrimary          = Color(0xFF000000),
    primaryContainer   = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFF5F5F5),

    secondary          = Color(0xFFA3A3A3),
    onSecondary        = Color(0xFF000000),
    secondaryContainer = Color(0xFF141414),
    onSecondaryContainer = Color(0xFFA3A3A3),

    tertiary           = Color(0xFF737373),
    onTertiary         = Color(0xFF000000),
    tertiaryContainer  = Color(0xFF141414),
    onTertiaryContainer = Color(0xFFA3A3A3),

    error              = Color(0xFF6B3A3A),
    onError            = Color(0xFFF5F5F5),
    errorContainer     = Color(0xFF1A0A0A),
    onErrorContainer   = Color(0xFFC17070),

    background         = DarkBg,
    onBackground       = DarkOnBg,
    surface            = DarkSurface,
    onSurface          = DarkOnBg,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = DarkOnBgVar,
    outline            = DarkBorder,
    outlineVariant     = DarkSurface3
)

// ── Material3 Theme ──

@Composable
fun KeycePassTheme(
    darkTheme: Boolean = true,  // Defaut: dark (comme Linear/Stripe)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
