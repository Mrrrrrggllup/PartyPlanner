package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.repository.ItemRepository

class MarkItemsSeenUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int): Result<Unit> = repository.markItemsSeen(eventId)
}
