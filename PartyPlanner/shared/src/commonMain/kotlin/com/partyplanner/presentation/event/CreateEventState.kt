package com.partyplanner.presentation.event

sealed class CreateEventState {
    data object Idle : CreateEventState()
    data object Loading : CreateEventState()
    data object Success : CreateEventState()
    data class Error(val message: String) : CreateEventState()
}
