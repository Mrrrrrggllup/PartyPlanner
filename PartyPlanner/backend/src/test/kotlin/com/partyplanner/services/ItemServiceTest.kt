package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.AddItemBroughtDto
import com.partyplanner.dto.AddItemRequestDto
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemServiceTest {

    private val service = ItemService(NotificationService(""))

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    @Test
    fun `addItemRequest stores item and returns response`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val response = service.addItemRequest(eventId, owner, AddItemRequestDto("Chips", 2))
        assertEquals("Chips", response.label)
        assertEquals(2, response.quantity)
        assertFalse(response.isFulfilled)
    }

    @Test
    fun `addItemRequest denies non-participant`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.addItemRequest(eventId, stranger, AddItemRequestDto("Chips", 1))
        }
    }

    @Test
    fun `fulfillRequest marks item as fulfilled and assigns user`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)
        val item = service.addItemRequest(eventId, owner, AddItemRequestDto("Vin", 1))

        val fulfilled = service.fulfillRequest(eventId, item.id, owner)
        assertTrue(fulfilled.isFulfilled)
        assertNotNull(fulfilled.assignedToName)
    }

    @Test
    fun `fulfillRequest toggles back when called by assignee`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)
        val item = service.addItemRequest(eventId, owner, AddItemRequestDto("Vin", 1))

        service.fulfillRequest(eventId, item.id, owner)
        val unfulfilled = service.fulfillRequest(eventId, item.id, owner)
        assertFalse(unfulfilled.isFulfilled)
        assertNull(unfulfilled.assignedToName)
    }

    @Test
    fun `fulfillRequest denies un-fulfill by non-assignee non-owner`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val item = service.addItemRequest(eventId, owner, AddItemRequestDto("Vin", 1))
        service.fulfillRequest(eventId, item.id, owner)

        assertFailsWith<IllegalArgumentException> {
            service.fulfillRequest(eventId, item.id, guest)
        }
    }

    @Test
    fun `deleteItemRequest succeeds for owner`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)
        val item = service.addItemRequest(eventId, owner, AddItemRequestDto("Chips", 1))

        service.deleteItemRequest(eventId, item.id, owner)

        val items = service.getItems(eventId, owner)
        assertTrue(items.requests.none { it.id == item.id })
    }

    @Test
    fun `deleteItemRequest denies non-owner non-requester`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val item = service.addItemRequest(eventId, owner, AddItemRequestDto("Chips", 1))

        assertFailsWith<IllegalArgumentException> {
            service.deleteItemRequest(eventId, item.id, guest)
        }
    }

    @Test
    fun `addItemBrought stores item and returns response`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val response = service.addItemBrought(eventId, owner, AddItemBroughtDto("Bière", 6))
        assertEquals("Bière", response.label)
        assertEquals(6, response.quantity)
        assertEquals(owner, response.userId)
    }

    @Test
    fun `deleteItemBrought denies non-owner non-author`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val item = service.addItemBrought(eventId, owner, AddItemBroughtDto("Vin", 1))

        assertFailsWith<IllegalArgumentException> {
            service.deleteItemBrought(eventId, item.id, guest)
        }
    }
}
