package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.model.ItemBrought
import com.partyplanner.domain.repository.ItemRepository

class AddItemBroughtUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int, label: String, quantity: Int, categoryId: Int? = null): Result<ItemBrought> =
        repository.addItemBrought(eventId, label, quantity, categoryId)
}
