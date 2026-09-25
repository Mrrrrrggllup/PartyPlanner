package com.partyplanner.fake

import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.model.ItemBrought
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.model.ItemRequest
import com.partyplanner.domain.repository.ItemRepository

internal class FakeItemRepository(
    private val getItemsResult: Result<EventItems> = Result.success(EventItems(emptyList(), emptyList())),
    private val addRequestResult: Result<ItemRequest> = Result.success(testItemRequest),
    private val fulfillResult: Result<ItemRequest> = Result.success(testItemRequest.copy(isFulfilled = true)),
    private val deleteRequestResult: Result<Unit> = Result.success(Unit),
    private val addBroughtResult: Result<ItemBrought> = Result.success(
        ItemBrought(1, "Chips", 1, 1, "Test User", null, null, null)
    ),
    private val deleteBroughtResult: Result<Unit> = Result.success(Unit),
) : ItemRepository {

    var addRequestCalledWith: Triple<Int, String, Int>? = null
    var fulfillCalledWith: Pair<Int, Int>? = null

    override suspend fun getCategories(): Result<List<ItemCategory>> = Result.success(emptyList())

    override suspend fun getItems(eventId: Int): Result<EventItems> = getItemsResult

    override suspend fun addItemRequest(
        eventId: Int,
        label: String,
        quantity: Int,
        categoryId: Int?,
    ): Result<ItemRequest> {
        addRequestCalledWith = Triple(eventId, label, quantity)
        return addRequestResult
    }

    override suspend fun fulfillRequest(eventId: Int, requestId: Int): Result<ItemRequest> {
        fulfillCalledWith = Pair(eventId, requestId)
        return fulfillResult
    }

    override suspend fun deleteItemRequest(eventId: Int, requestId: Int): Result<Unit> = deleteRequestResult

    override suspend fun addItemBrought(
        eventId: Int,
        label: String,
        quantity: Int,
        categoryId: Int?,
    ): Result<ItemBrought> = addBroughtResult

    override suspend fun deleteItemBrought(eventId: Int, broughtId: Int): Result<Unit> = deleteBroughtResult

    override suspend fun markItemsSeen(eventId: Int): Result<Unit> = Result.success(Unit)
}
