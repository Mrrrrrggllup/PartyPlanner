package com.partyplanner.domain.repository

import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.model.ItemBrought
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.model.ItemRequest

interface ItemRepository {
    suspend fun getCategories(): Result<List<ItemCategory>>
    suspend fun getItems(eventId: Int): Result<EventItems>
    suspend fun addItemRequest(eventId: Int, label: String, quantity: Int, categoryId: Int?): Result<ItemRequest>
    suspend fun fulfillRequest(eventId: Int, requestId: Int): Result<ItemRequest>
    suspend fun deleteItemRequest(eventId: Int, requestId: Int): Result<Unit>
    suspend fun addItemBrought(eventId: Int, label: String, quantity: Int, categoryId: Int?): Result<ItemBrought>
    suspend fun deleteItemBrought(eventId: Int, broughtId: Int): Result<Unit>
}
