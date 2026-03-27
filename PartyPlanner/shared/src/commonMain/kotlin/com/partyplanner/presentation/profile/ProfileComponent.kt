package com.partyplanner.presentation.profile

import kotlinx.coroutines.flow.StateFlow

interface ProfileComponent {
    val state: StateFlow<ProfileState>
    fun onThemeChange(mode: ThemeMode)
    fun onLogout()
    fun onBack()
}
