package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.repository.ItemRepository

class DeleteItemBroughtUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(eventId: Int, broughtId: Int): Result<Unit> =
        repository.deleteItemBrought(eventId, broughtId)
}
