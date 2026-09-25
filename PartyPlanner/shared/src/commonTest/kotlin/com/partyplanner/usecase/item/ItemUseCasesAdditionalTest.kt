package com.partyplanner.usecase.item

import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.model.ItemBrought
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.usecase.item.AddItemBroughtUseCase
import com.partyplanner.domain.usecase.item.DeleteItemBroughtUseCase
import com.partyplanner.domain.usecase.item.DeleteItemRequestUseCase
import com.partyplanner.domain.usecase.item.GetCategoriesUseCase
import com.partyplanner.domain.usecase.item.GetItemsUseCase
import com.partyplanner.domain.usecase.item.MarkItemsSeenUseCase
import com.partyplanner.fake.FakeItemRepository
import com.partyplanner.fake.testItemRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetItemsUseCaseTest {

    @Test
    fun `returns items on success`() = runTest {
        val items = EventItems(listOf(testItemRequest), emptyList())
        val result = GetItemsUseCase(FakeItemRepository(getItemsResult = Result.success(items)))(eventId = 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.requests?.size)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = GetItemsUseCase(
            FakeItemRepository(getItemsResult = Result.failure(Exception("Access denied")))
        )(1)
        assertTrue(result.isFailure)
    }
}

class DeleteItemRequestUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = DeleteItemRequestUseCase(FakeItemRepository())(eventId = 1, requestId = 10)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates access denied`() = runTest {
        val result = DeleteItemRequestUseCase(
            FakeItemRepository(deleteRequestResult = Result.failure(Exception("Access denied")))
        )(1, 10)
        assertTrue(result.isFailure)
        assertEquals("Access denied", result.exceptionOrNull()?.message)
    }
}

class DeleteItemBroughtUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = DeleteItemBroughtUseCase(FakeItemRepository())(eventId = 1, broughtId = 5)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = DeleteItemBroughtUseCase(
            FakeItemRepository(deleteBroughtResult = Result.failure(Exception("Not found")))
        )(1, 5)
        assertTrue(result.isFailure)
    }
}

class AddItemBroughtUseCaseTest {

    @Test
    fun `returns created item on success`() = runTest {
        val expected = ItemBrought(2, "Vin", 1, 1, "Alice", null, null, null)
        val result = AddItemBroughtUseCase(FakeItemRepository(addBroughtResult = Result.success(expected)))(
            eventId = 1, label = "Vin", quantity = 1
        )
        assertTrue(result.isSuccess)
        assertEquals("Vin", result.getOrNull()?.label)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = AddItemBroughtUseCase(
            FakeItemRepository(addBroughtResult = Result.failure(Exception("Access denied")))
        )(1, "Vin", 1)
        assertTrue(result.isFailure)
    }
}

class GetCategoriesUseCaseTest {

    @Test
    fun `returns category list on success`() = runTest {
        val cats = listOf(ItemCategory(1, "Nourriture", "🍕"))
        val result = GetCategoriesUseCase(
            FakeItemRepository(getItemsResult = Result.success(EventItems(emptyList(), emptyList())))
                .let {
                    object : com.partyplanner.domain.repository.ItemRepository by it {
                        override suspend fun getCategories() = Result.success(cats)
                    }
                }
        )()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = GetCategoriesUseCase(
            FakeItemRepository().let {
                object : com.partyplanner.domain.repository.ItemRepository by it {
                    override suspend fun getCategories() = Result.failure<List<ItemCategory>>(Exception("Error"))
                }
            }
        )()
        assertTrue(result.isFailure)
    }
}

class MarkItemsSeenUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = MarkItemsSeenUseCase(FakeItemRepository())(eventId = 1)
        assertTrue(result.isSuccess)
    }
}
