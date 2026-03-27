package com.partyplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// ── CompositionLocal pour les tokens extra (gradients, accents) ───────────────

private val LocalAppColors = staticCompositionLocalOf<AppExtraColors> {
    error("AppTheme not provided — wrap your UI with AppTheme { ... }")
}

/** Accès aux tokens extra depuis n'importe quel Composable : MaterialTheme.appColors */
val MaterialTheme.appColors: AppExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

// ── Entry point du thème ──────────────────────────────────────────────────────

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalAppColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            shapes      = AppMaterialShapes,
            content     = content,
        )
    }
}