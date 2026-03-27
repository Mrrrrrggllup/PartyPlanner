package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.repository.ItemRepository

class DeleteItemRequestUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int, requestId: Int): Result<Unit> =
        repository.deleteItemRequest(eventId, requestId)
}
