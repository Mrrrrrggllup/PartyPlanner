package com.partyplanner.presentation.home

import com.partyplanner.domain.model.Event

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val events: List<Event>, val currentUserId: Int, val displayName: String) : HomeState()
    data class Error(val message: String) : HomeState()
}
