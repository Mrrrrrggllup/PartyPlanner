package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.CreateCarpoolOfferDto
import com.partyplanner.dto.JoinCarpoolDto
import com.partyplanner.dto.UpdateCarpoolOfferDto
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CarpoolServiceTest {

    private val service = CarpoolService(NotificationService(""))

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    @Test
    fun `createOffer stores offer and returns response`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))
        assertEquals(3, offer.seatsAvailable)
        assertEquals(3, offer.seatsRemaining)
        assertEquals(owner, offer.driverId)
    }

    @Test
    fun `createOffer rejects zero seats`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 0))
        }
    }

    @Test
    fun `updateOffer rejects non-driver`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))

        assertFailsWith<IllegalArgumentException> {
            service.updateOffer(eventId, offer.id, guest, UpdateCarpoolOfferDto(seatsAvailable = 5))
        }
    }

    @Test
    fun `joinOffer decreases seatsRemaining`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId = eventId, userId = guest, status = InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))
        val joined = service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())

        assertEquals(2, joined.seatsRemaining)
    }

    @Test
    fun `joinOffer driver cannot join own offer`() = runTest {
        val owner = createTestUser()
        val eventId = createTestEvent(ownerId = owner)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))

        assertFailsWith<IllegalArgumentException> {
            service.joinOffer(eventId, offer.id, owner, JoinCarpoolDto())
        }
    }

    @Test
    fun `joinOffer full offer throws`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest1 = createTestUser(email = "g1@test.com")
        val guest2 = createTestUser(email = "g2@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest1, InvitationStatus.ACCEPTED)
        addInvitation(eventId, guest2, InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 1))
        service.joinOffer(eventId, offer.id, guest1, JoinCarpoolDto())

        assertFailsWith<IllegalArgumentException> {
            service.joinOffer(eventId, offer.id, guest2, JoinCarpoolDto())
        }
    }

    @Test
    fun `joinOffer already-joined throws`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest, InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))
        service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())

        assertFailsWith<IllegalArgumentException> {
            service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())
        }
    }

    @Test
    fun `leaveOffer after join restores seatsRemaining`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest, InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))
        service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())
        val left = service.leaveOffer(eventId, offer.id, guest)

        assertEquals(3, left.seatsRemaining)
    }

    @Test
    fun `leaveOffer non-passenger throws`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest, InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))

        assertFailsWith<IllegalStateException> {
            service.leaveOffer(eventId, offer.id, guest)
        }
    }

    @Test
    fun `CANCELLED passenger can rejoin`() = runTest {
        val owner = createTestUser(email = "owner@test.com")
        val guest = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest, InvitationStatus.ACCEPTED)

        val offer = service.createOffer(eventId, owner, CreateCarpoolOfferDto(seatsAvailable = 3))
        service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())
        service.leaveOffer(eventId, offer.id, guest)

        val rejoined = service.joinOffer(eventId, offer.id, guest, JoinCarpoolDto())
        assertTrue(rejoined.passengers.any { it.passengerId == guest })
    }
}
