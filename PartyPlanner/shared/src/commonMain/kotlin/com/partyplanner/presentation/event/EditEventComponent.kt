package com.partyplanner.presentation.event

import com.partyplanner.domain.model.Event
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

sealed class EditEventState {
    data object Loading : EditEventState()
    data class Loaded(
        val event: Event,
        val isSaving: Boolean = false,
        val error: String? = null,
    ) : EditEventState()
    data class Error(val message: String) : EditEventState()
}

interface EditEventComponent {
    val state: StateFlow<EditEventState>
    fun onBack()
    fun onSave(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    )
}
