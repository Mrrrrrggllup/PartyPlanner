package com.partyplanner.domain.usecase.item

import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.repository.ItemRepository

class GetCategoriesUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(): Result<List<ItemCategory>> = repository.getCategories()
}
