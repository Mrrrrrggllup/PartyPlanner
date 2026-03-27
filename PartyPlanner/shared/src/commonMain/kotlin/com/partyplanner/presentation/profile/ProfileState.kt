package com.partyplanner.presentation.profile

sealed class ProfileState {
    data object Loading : ProfileState()
    data class Success(
        val displayName: String,
        val currentTheme: ThemeMode,
    ) : ProfileState()
}
