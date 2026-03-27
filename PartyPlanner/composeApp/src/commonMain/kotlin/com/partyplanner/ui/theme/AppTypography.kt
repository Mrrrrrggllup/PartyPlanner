package com.partyplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Font families ─────────────────────────────────────────────────────────────
//
// Pour ajouter les vraies fonts :
//   1. Télécharger depuis Google Fonts :
//      • Syne           → https://fonts.google.com/specimen/Syne
//      • Plus Jakarta Sans → https://fonts.google.com/specimen/Plus+Jakarta+Sans
//   2. Placer les .ttf dans : composeApp/src/commonMain/composeResources/font/
//      Exemple : Syne-Bold.ttf, Syne-ExtraBold.ttf,
//                PlusJakartaSans-Regular.ttf, PlusJakartaSans-SemiBold.ttf
//   3. Remplacer les deux lignes ci-dessous par :
//
//   val SyneFontFamily = FontFamily(
//       Font(Res.font.Syne_Bold, weight = FontWeight.Bold),
//       Font(Res.font.Syne_ExtraBold, weight = FontWeight.ExtraBold),
//   )
//   val JakartaFontFamily = FontFamily(
//       Font(Res.font.PlusJakartaSans_Regular, weight = FontWeight.Normal),
//       Font(Res.font.PlusJakartaSans_Medium, weight = FontWeight.Medium),
//       Font(Res.font.PlusJakartaSans_SemiBold, weight = FontWeight.SemiBold),
//   )

val SyneFontFamily: FontFamily = FontFamily.Default    // TODO: remplacer (voir ci-dessus)
val JakartaFontFamily: FontFamily = FontFamily.Default // TODO: remplacer (voir ci-dessus)

// ── Typography scale ──────────────────────────────────────────────────────────

val AppTypography = Typography(
    // Logo / gros titres d'écran
    displayLarge = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Hero banner titre
    displayMedium = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Nom d'app dans la topbar
    displaySmall = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Titres de section, nom d'événement
    titleLarge = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Sous-titres
    titleMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    ),
    // Corps de texte principal
    bodyLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
    // Labels, chips, badges
    labelLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.05.sp,
    ),
    // Nav items, eyebrow text (uppercase appliqué côté UI)
    labelSmall = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.08.sp,
    ),
)