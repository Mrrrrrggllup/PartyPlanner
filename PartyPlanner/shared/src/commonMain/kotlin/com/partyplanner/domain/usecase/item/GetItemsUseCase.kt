package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.repository.ItemRepository

class GetItemsUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int): Result<EventItems> = repository.getItems(eventId)
}
