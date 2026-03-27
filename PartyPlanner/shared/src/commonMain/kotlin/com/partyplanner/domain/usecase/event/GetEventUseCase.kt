package com.partyplanner.domain.usecase.event

import com.partyplanner.domain.model.Event
import com.partyplanner.domain.repository.EventRepository

class GetEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: Int): Result<Event> = repository.getEvent(id)
}
