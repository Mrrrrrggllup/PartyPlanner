package com.partyplanner

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.partyplanner.presentation.profile.ThemeManager
import com.partyplanner.presentation.profile.ThemeMode
import com.partyplanner.presentation.root.RootComponent
import com.partyplanner.ui.theme.AppTheme

@Composable
fun App(root: RootComponent) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    AppTheme(darkTheme = darkTheme) {
        RootContent(root)
    }
}
