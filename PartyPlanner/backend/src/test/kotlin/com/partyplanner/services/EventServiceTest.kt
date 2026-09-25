package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.CreateEventRequest
import com.partyplanner.dto.UpdateEventRequest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventServiceTest {

    private val service = EventService()

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    @Test
    fun `createEvent stores event and returns response with token`() = runTest {
        val userId = createTestUser()
        val response = service.createEvent(
            userId,
            CreateEventRequest(
                title = "Birthday Party",
                startDate = LocalDateTime(2026, 10, 1, 18, 0),
            )
        )
        assertEquals("Birthday Party", response.title)
        assertEquals(userId, response.ownerId)
        assertNotNull(response.inviteToken)
    }

    @Test
    fun `getEventsForUser returns owned and invited events`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val ownerEvents = service.getEventsForUser(owner)
        val guestEvents = service.getEventsForUser(guest)

        assertTrue(ownerEvents.any { it.id == eventId })
        assertTrue(guestEvents.any { it.id == eventId })
    }

    @Test
    fun `getEvent denies access for non-participant`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.getEvent(eventId, stranger)
        }
    }

    @Test
    fun `getEvent allows access for invited user`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val response = service.getEvent(eventId, guest)
        assertEquals(eventId, response.id)
    }

    @Test
    fun `updateEvent succeeds for owner`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val response = service.updateEvent(eventId, owner, UpdateEventRequest(title = "New Title"))
        assertEquals("New Title", response.title)
    }

    @Test
    fun `updateEvent denies non-owner`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val other = createTestUser(email = "other@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.updateEvent(eventId, other, UpdateEventRequest(title = "Hack"))
        }
    }

    @Test
    fun `deleteEvent removes event and cascades`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        service.deleteEvent(eventId, owner)

        assertFailsWith<IllegalStateException> {
            service.getEvent(eventId, owner)
        }
    }

    @Test
    fun `deleteEvent denies non-owner`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val other = createTestUser(email = "other@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.deleteEvent(eventId, other)
        }
    }
}
