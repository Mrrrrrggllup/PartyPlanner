package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.AddItemBroughtDto
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationServiceTest {

    private val service = InvitationService(NotificationService(""))
    private val itemService = ItemService(NotificationService(""))

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    @Test
    fun `getInviteInfo returns event info for valid token`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val token = transaction {
            com.partyplanner.db.tables.EventEntity.findById(eventId)!!.inviteToken!!
        }

        val info = service.getInviteInfo(token, owner)
        assertEquals(eventId, info.eventId)
        assertTrue(info.isOwner)
        assertNull(info.currentStatus)
    }

    @Test
    fun `getInviteInfo throws for invalid token`() = runTest {
        assertFailsWith<IllegalStateException> {
            service.getInviteInfo("invalid-token", 1)
        }
    }

    @Test
    fun `rsvp ACCEPTED creates invitation`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)

        val token = transaction {
            com.partyplanner.db.tables.EventEntity.findById(eventId)!!.inviteToken!!
        }

        val info = service.rsvp(token, guest, "ACCEPTED")
        assertEquals("ACCEPTED", info.currentStatus)
    }

    @Test
    fun `rsvp owner cannot invite themselves`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)
        val token = transaction {
            com.partyplanner.db.tables.EventEntity.findById(eventId)!!.inviteToken!!
        }

        assertFailsWith<IllegalArgumentException> {
            service.rsvp(token, owner, "ACCEPTED")
        }
    }

    @Test
    fun `rsvp DECLINED cascades to delete items brought`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        itemService.addItemBrought(eventId, guest, AddItemBroughtDto("Bière", 6))
        val itemsBefore = itemService.getItems(eventId, guest)
        assertTrue(itemsBefore.brought.any { it.userId == guest })

        val token = transaction {
            com.partyplanner.db.tables.EventEntity.findById(eventId)!!.inviteToken!!
        }
        service.rsvp(token, guest, "DECLINED")

        val itemsAfter = itemService.getItems(eventId, owner)
        assertTrue(itemsAfter.brought.none { it.userId == guest })
    }

    @Test
    fun `inviteByUserId creates pending invitation`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)

        val inv = service.inviteByUserId(eventId, owner, guest)
        assertEquals(guest, inv.userId)
        assertEquals("PENDING", inv.status)
    }

    @Test
    fun `inviteByUserId self-invite throws`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.inviteByUserId(eventId, owner, owner)
        }
    }

    @Test
    fun `inviteByUserId duplicate throws`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)

        service.inviteByUserId(eventId, owner, guest)

        assertFailsWith<IllegalArgumentException> {
            service.inviteByUserId(eventId, owner, guest)
        }
    }

    @Test
    fun `inviteByEmail creates pending invitation`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)

        val inv = service.inviteByEmail(eventId, owner, "guest@test.com")
        assertEquals(guest, inv.userId)
        assertEquals("PENDING", inv.status)
    }

    @Test
    fun `inviteByEmail unknown email throws`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalStateException> {
            service.inviteByEmail(eventId, owner, "nobody@test.com")
        }
    }

    @Test
    fun `getEventInvitations non-participant access denied`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.getEventInvitations(eventId, stranger)
        }
    }

    @Test
    fun `getInviteSuggestions returns users from other events`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")

        val event1 = createTestEvent(ownerId = owner, title = "Event 1")
        addInvitation(event1, guest, InvitationStatus.ACCEPTED)

        val event2 = createTestEvent(ownerId = owner, title = "Event 2")
        val suggestions = service.getInviteSuggestions(event2, owner)

        assertTrue(suggestions.any { it.id == guest })
    }
}
