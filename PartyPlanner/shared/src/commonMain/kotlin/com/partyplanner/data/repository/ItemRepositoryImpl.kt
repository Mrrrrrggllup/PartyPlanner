package com.partyplanner.data.repository

import com.partyplanner.data.remote.ItemApi
import com.partyplanner.data.remote.dto.*
import com.partyplanner.domain.model.*
import com.partyplanner.domain.repository.ItemRepository

class ItemRepositoryImpl(private val api: ItemApi) : ItemRepository {

    override suspend fun getCategories(): Result<List<ItemCategory>> = runCatching {
        api.getCategories().map { it.toDomain() }
    }

    override suspend fun getItems(eventId: Int): Result<EventItems> = runCatching {
        val response = api.getItems(eventId)
        EventItems(
            requests = response.requests.map { it.toDomain() },
            brought  = response.brought.map { it.toDomain() },
        )
    }

    override suspend fun addItemRequest(eventId: Int, label: String, quantity: Int, categoryId: Int?): Result<ItemRequest> = runCatching {
        api.addItemRequest(eventId, AddItemRequestDto(label, quantity, categoryId)).toDomain()
    }

    override suspend fun fulfillRequest(eventId: Int, requestId: Int): Result<ItemRequest> = runCatching {
        api.fulfillRequest(eventId, requestId).toDomain()
    }

    override suspend fun deleteItemRequest(eventId: Int, requestId: Int): Result<Unit> = runCatching {
        api.deleteItemRequest(eventId, requestId)
    }

    override suspend fun addItemBrought(eventId: Int, label: String, quantity: Int, categoryId: Int?): Result<ItemBrought> = runCatching {
        api.addItemBrought(eventId, AddItemBroughtDto(label, quantity, categoryId)).toDomain()
    }

    override suspend fun deleteItemBrought(eventId: Int, broughtId: Int): Result<Unit> = runCatching {
        api.deleteItemBrought(eventId, broughtId)
    }

    private fun CategoryResponse.toDomain()     = ItemCategory(id, label, icon)
    private fun ItemRequestResponse.toDomain()  = ItemRequest(id, label, quantity, isFulfilled, assignedToName, categoryId, categoryLabel, categoryIcon)
    private fun ItemBroughtResponse.toDomain()  = ItemBrought(id, label, quantity, userId, userName, categoryId, categoryLabel, categoryIcon)
}
