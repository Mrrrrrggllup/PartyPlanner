package com.partyplanner.domain.usecase.event

import com.partyplanner.domain.model.Event
import com.partyplanner.domain.repository.EventRepository
import kotlinx.datetime.LocalDateTime

class UpdateEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(
        id: Int,
        title: String?,
        description: String?,
        location: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ): Result<Event> = repository.updateEvent(id, title, description, location, startDate, endDate)
}
