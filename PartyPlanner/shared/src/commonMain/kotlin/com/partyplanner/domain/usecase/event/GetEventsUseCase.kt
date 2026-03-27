package com.partyplanner.domain.usecase.event

import com.partyplanner.domain.model.Event
import com.partyplanner.domain.repository.EventRepository

class GetEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): Result<List<Event>> = repository.getEvents()
}
