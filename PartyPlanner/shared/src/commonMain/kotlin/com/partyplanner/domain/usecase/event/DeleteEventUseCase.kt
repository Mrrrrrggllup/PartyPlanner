package com.partyplanner.domain.usecase.event

import com.partyplanner.domain.repository.EventRepository

class DeleteEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: Int): Result<Unit> = repository.deleteEvent(id)
}
