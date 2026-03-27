package com.partyplanner.presentation.event

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

interface CreateEventComponent {
    val state: StateFlow<CreateEventState>
    fun onBack()
    fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    )
}
