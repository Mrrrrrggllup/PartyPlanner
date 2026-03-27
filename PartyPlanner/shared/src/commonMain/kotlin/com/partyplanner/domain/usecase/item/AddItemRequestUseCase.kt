package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.model.ItemRequest
import com.partyplanner.domain.repository.ItemRepository

class AddItemRequestUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int, label: String, quantity: Int, categoryId: Int? = null): Result<ItemRequest> =
        repository.addItemRequest(eventId, label, quantity, categoryId)
}
