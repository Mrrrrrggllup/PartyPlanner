package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.CarpoolPassengerStatus
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.AddContributionDto
import com.partyplanner.dto.AddItemBroughtDto
import com.partyplanner.dto.AddItemRequestDto
import com.partyplanner.dto.CreateCarpoolOfferDto
import com.partyplanner.dto.JoinCarpoolDto
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationServiceTest {

    private val service              = InvitationService(NotificationService(""))
    private val itemService          = ItemService(NotificationService(""))
    private val carpoolService       = CarpoolService(NotificationService(""))
    private val contributionService  = ContributionService()

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

    // ── removeGuest ────────────────────────────────────────────────────────────

    @Test
    fun `removeGuest non-owner is denied`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        val inv = service.inviteByUserId(eventId, owner, guest)

        assertFailsWith<IllegalArgumentException> {
            service.removeGuest(eventId, inv.id, guest) // guest trying to remove themselves
        }
    }

    @Test
    fun `removeGuest clean guest — invitation deleted and re-invite works`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        val inv = service.inviteByUserId(eventId, owner, guest)

        service.removeGuest(eventId, inv.id, owner)

        // Invitation no longer appears in the list
        val invitations = service.getEventInvitations(eventId, owner)
        assertTrue(invitations.none { it.userId == guest })

        // Can re-invite the same guest
        val reInvited = service.inviteByUserId(eventId, owner, guest)
        assertEquals(guest, reInvited.userId)
        assertEquals("PENDING", reInvited.status)
    }

    @Test
    fun `removeGuest driver — offer and all its passengers are deleted`() = runTest {
        val owner     = createTestUser(email = "owner@test.com")
        val driver    = createTestUser(email = "driver@test.com")
        val passenger = createTestUser(email = "passenger@test.com")
        val eventId   = createTestEvent(ownerId = owner)
        // inviteByUserId creates the invitation (PENDING is enough for checkAccess)
        val driverInv = service.inviteByUserId(eventId, owner, driver)
        service.inviteByUserId(eventId, owner, passenger)

        // Driver creates a 2-seat offer and passenger joins
        val offer = carpoolService.createOffer(eventId, driver, CreateCarpoolOfferDto(seatsAvailable = 2))
        carpoolService.joinOffer(eventId, offer.id, passenger, JoinCarpoolDto())

        // Sanity: passenger is in the offer before removal
        val before = carpoolService.getOffers(eventId, owner)
        assertTrue(before.offers.any { o -> o.id == offer.id && o.passengers.any { it.passengerId == passenger } })

        // Remove the driver
        service.removeGuest(eventId, driverInv.id, owner)

        // Offer is gone entirely — passenger loses their ride
        val after = carpoolService.getOffers(eventId, owner)
        assertTrue(after.offers.none { it.id == offer.id })
    }

    @Test
    fun `removeGuest passenger — seat is freed and offer still exists`() = runTest {
        val owner     = createTestUser(email = "owner@test.com")
        val driver    = createTestUser(email = "driver@test.com")
        val passenger = createTestUser(email = "passenger@test.com")
        val eventId   = createTestEvent(ownerId = owner)
        service.inviteByUserId(eventId, owner, driver)
        val passengerInv = service.inviteByUserId(eventId, owner, passenger)

        val offer = carpoolService.createOffer(eventId, driver, CreateCarpoolOfferDto(seatsAvailable = 2))
        carpoolService.joinOffer(eventId, offer.id, passenger, JoinCarpoolDto())

        val before = carpoolService.getOffers(eventId, owner)
        assertEquals(1, before.offers.first { it.id == offer.id }.seatsRemaining)

        // Remove the passenger
        service.removeGuest(eventId, passengerInv.id, owner)

        // Offer still exists but the seat is freed
        val after = carpoolService.getOffers(eventId, owner)
        val updatedOffer = after.offers.first { it.id == offer.id }
        assertEquals(2, updatedOffer.seatsRemaining)
        assertTrue(updatedOffer.passengers.none { it.passengerId == passenger })
    }

    @Test
    fun `removeGuest with items brought and fulfilled request — items deleted and request reset`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        val inv = service.inviteByUserId(eventId, owner, guest)

        // Guest adds an item they're bringing
        itemService.addItemBrought(eventId, guest, AddItemBroughtDto("Bière", 6))

        // Owner creates a needed item, guest fulfills it
        val request = itemService.addItemRequest(eventId, owner, AddItemRequestDto("Chips", 2))
        itemService.fulfillRequest(eventId, request.id, guest)

        // Sanity checks before removal
        val itemsBefore = itemService.getItems(eventId, owner)
        assertTrue(itemsBefore.brought.any { it.userId == guest })
        assertTrue(itemsBefore.requests.first { it.id == request.id }.isFulfilled)

        // Remove the guest
        service.removeGuest(eventId, inv.id, owner)

        val itemsAfter = itemService.getItems(eventId, owner)

        // ItemsBrought deleted
        assertTrue(itemsAfter.brought.none { it.userId == guest })

        // ItemRequest reset to unfulfilled, no longer assigned
        val resetRequest = itemsAfter.requests.first { it.id == request.id }
        assertFalse(resetRequest.isFulfilled)
        assertNull(resetRequest.assignedToName)
    }

    @Test
    fun `removeGuest with contributions — all contributions added by or linked to guest are deleted`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        val inv     = service.inviteByUserId(eventId, owner, guest)

        // Contribution added BY the guest (linked to owner)
        contributionService.addContribution(eventId, guest, AddContributionDto(linkedUserId = owner, label = "Glace", amount = 5.0))
        // Contribution added by owner, LINKED TO the guest
        contributionService.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Chips", amount = 3.0))

        val before = contributionService.getContributions(eventId, owner)
        assertEquals(2, before.size)

        service.removeGuest(eventId, inv.id, owner)

        val after = contributionService.getContributions(eventId, owner)
        assertTrue(after.isEmpty())
    }
}
