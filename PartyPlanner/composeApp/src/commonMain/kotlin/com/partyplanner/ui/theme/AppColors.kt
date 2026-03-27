package com.partyplanner.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Raw color tokens ─────────────────────────────────────────────────────────

object LightTokens {
    val Background      = Color(0xFFFDF8FF)
    val Surface         = Color(0xFFFFFFFF)
    val Surface2        = Color(0xFFF5EFFE)
    val Text            = Color(0xFF1A0A2E)
    val Muted           = Color(0xFF7B6A99)
    val Pink            = Color(0xFFF0147A)
    val Purple          = Color(0xFF7C3AED)
    val Yellow          = Color(0xFFF59E0B)
    val Cyan            = Color(0xFF0891B2)
    val Orange          = Color(0xFFEA580C)
    val Green           = Color(0xFF16A34A)
    val Border          = Color(0x1F8B5CF6) // rgba(139,92,246,0.12)
    val BorderHard      = Color(0x338B5CF6) // rgba(139,92,246,0.20)
    val PinkContainer   = Color(0xFFFDE8F4)
    val PurpleContainer = Color(0xFFEDE8FC)
}

object DarkTokens {
    val Background      = Color(0xFF0F0A1E)
    val Surface         = Color(0xFF1A1330)
    val Surface2        = Color(0xFF231A3D)
    val Text            = Color(0xFFF0EBF8)
    val Muted           = Color(0x7FF0EBF8) // rgba(240,235,248,0.50)
    val Pink            = Color(0xFFFF3D8A)
    val Purple          = Color(0xFF8B5CF6)
    val Yellow          = Color(0xFFFFD60A)
    val Cyan            = Color(0xFF00E5CC)
    val Orange          = Color(0xFFFF6B35)
    val Border          = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
    val PinkContainer   = Color(0xFF3D1628)
    val PurpleContainer = Color(0xFF1E1040)
}

// ── Material3 ColorScheme ─────────────────────────────────────────────────────

val AppLightColorScheme = lightColorScheme(
    primary              = LightTokens.Pink,
    onPrimary            = Color.White,
    primaryContainer     = LightTokens.PinkContainer,
    onPrimaryContainer   = LightTokens.Pink,
    secondary            = LightTokens.Purple,
    onSecondary          = Color.White,
    secondaryContainer   = LightTokens.PurpleContainer,
    onSecondaryContainer = LightTokens.Purple,
    tertiary             = LightTokens.Yellow,
    onTertiary           = LightTokens.Text,
    background           = LightTokens.Background,
    onBackground         = LightTokens.Text,
    surface              = LightTokens.Surface,
    onSurface            = LightTokens.Text,
    surfaceVariant       = LightTokens.Surface2,
    onSurfaceVariant     = LightTokens.Muted,
    outline              = LightTokens.Border,
    outlineVariant       = LightTokens.BorderHard,
)

val AppDarkColorScheme = darkColorScheme(
    primary              = DarkTokens.Pink,
    onPrimary            = Color.White,
    primaryContainer     = DarkTokens.PinkContainer,
    onPrimaryContainer   = DarkTokens.Pink,
    secondary            = DarkTokens.Purple,
    onSecondary          = Color.White,
    secondaryContainer   = DarkTokens.PurpleContainer,
    onSecondaryContainer = DarkTokens.Purple,
    tertiary             = DarkTokens.Yellow,
    onTertiary           = DarkTokens.Text,
    background           = DarkTokens.Background,
    onBackground         = DarkTokens.Text,
    surface              = DarkTokens.Surface,
    onSurface            = DarkTokens.Text,
    surfaceVariant       = DarkTokens.Surface2,
    onSurfaceVariant     = DarkTokens.Muted,
    outline              = DarkTokens.Border,
    outlineVariant       = DarkTokens.Border,
)

// ── Extra tokens (gradients, accents) ────────────────────────────────────────
// Accessible via MaterialTheme.appColors (see AppTheme.kt)

data class AppExtraColors(
    val pink: Color,
    val purple: Color,
    val yellow: Color,
    val cyan: Color,
    val orange: Color,
    val muted: Color,
    val surface2: Color,
    val border: Color,
    // Gradient pink → purple (boutons principaux, hero banner)
    val gradA: Brush,
    // Gradient yellow → orange (badges secondaires)
    val gradB: Brush,
    // Gradient cyan → purple (badges tertiaires)
    val gradC: Brush,
)

val LightExtraColors = AppExtraColors(
    pink    = LightTokens.Pink,
    purple  = LightTokens.Purple,
    yellow  = LightTokens.Yellow,
    cyan    = LightTokens.Cyan,
    orange  = LightTokens.Orange,
    muted   = LightTokens.Muted,
    surface2 = LightTokens.Surface2,
    border  = LightTokens.Border,
    gradA   = Brush.linearGradient(listOf(LightTokens.Pink, LightTokens.Purple)),
    gradB   = Brush.linearGradient(listOf(LightTokens.Yellow, LightTokens.Orange)),
    gradC   = Brush.linearGradient(listOf(LightTokens.Cyan, LightTokens.Purple)),
)

val DarkExtraColors = AppExtraColors(
    pink    = DarkTokens.Pink,
    purple  = DarkTokens.Purple,
    yellow  = DarkTokens.Yellow,
    cyan    = DarkTokens.Cyan,
    orange  = DarkTokens.Orange,
    muted   = DarkTokens.Muted,
    surface2 = DarkTokens.Surface2,
    border  = DarkTokens.Border,
    gradA   = Brush.linearGradient(listOf(DarkTokens.Pink, DarkTokens.Purple)),
    gradB   = Brush.linearGradient(listOf(DarkTokens.Yellow, DarkTokens.Orange)),
    gradC   = Brush.linearGradient(listOf(DarkTokens.Cyan, DarkTokens.Purple)),
)