package com.partyplanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Semantic shape tokens ─────────────────────────────────────────────────────

object AppShapes {
    /** Avatar utilisateur — carré arrondi */
    val Avatar = RoundedCornerShape(12.dp)
    /** Cards événement */
    val Card = RoundedCornerShape(20.dp)
    /** Hero banner */
    val HeroBanner = RoundedCornerShape(24.dp)
    /** Boutons principaux */
    val Button = RoundedCornerShape(16.dp)
    /** Pills : chips, badges, day pills, nav tabs */
    val Pill = RoundedCornerShape(100.dp)
    /** Bouton retour / icône action (glassmorphism) */
    val ActionIcon = RoundedCornerShape(12.dp)
    /** Champs de texte — assez arrondi pour être joli, assez petit pour ne pas cropper le label */
    val TextField = RoundedCornerShape(8.dp)
}

// ── Material3 Shapes ──────────────────────────────────────────────────────────

val AppMaterialShapes = Shapes(
    extraSmall = AppShapes.ActionIcon,        // 12dp — petits éléments
    small      = AppShapes.Avatar,            // 12dp — avatars, badges
    medium     = AppShapes.Card,             // 20dp — cards
    large      = AppShapes.HeroBanner,       // 24dp — hero, bottom sheets
    extraLarge = RoundedCornerShape(28.dp),  // dialogs, AlertDialog, DatePickerDialog — NE PAS mettre Pill ici
)
// Note : AppShapes.Pill (100dp) est utilisé explicitement là où c'est voulu (boutons, chips, tabs)