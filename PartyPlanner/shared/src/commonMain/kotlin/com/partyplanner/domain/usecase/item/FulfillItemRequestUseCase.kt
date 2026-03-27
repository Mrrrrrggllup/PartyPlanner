package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.model.ItemRequest
import com.partyplanner.domain.repository.ItemRepository

class FulfillItemRequestUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int, requestId: Int): Result<ItemRequest> =
        repository.fulfillRequest(eventId, requestId)
}
