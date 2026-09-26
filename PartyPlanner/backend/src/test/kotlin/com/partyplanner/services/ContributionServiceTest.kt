package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.addInvitation
import com.partyplanner.createTestEvent
import com.partyplanner.createTestUser
import com.partyplanner.db.tables.Contributions
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.dto.AddContributionDto
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContributionServiceTest {

    private val service           = ContributionService()
    private val invitationService = InvitationService(NotificationService(""))

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    // ── access control ─────────────────────────────────────────────────────────

    @Test
    fun `getContributions - owner can read`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest)

        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Glace", amount = 5.0))

        val list = service.getContributions(eventId, owner)
        assertEquals(1, list.size)
        assertEquals("Glace", list[0].label)
    }

    @Test
    fun `getContributions - invited guest can read`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest)

        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = owner, label = "Charbon", amount = 12.0))

        val list = service.getContributions(eventId, guest)
        assertEquals(1, list.size)
    }

    @Test
    fun `getContributions - non-participant is denied`() = runTest {
        val owner    = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId  = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.getContributions(eventId, stranger)
        }
    }

    // ── addContribution ────────────────────────────────────────────────────────

    @Test
    fun `addContribution - participant can add for any participant`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest)

        val result = service.addContribution(
            eventId, guest,
            AddContributionDto(linkedUserId = owner, label = "Bière", amount = 18.50)
        )

        assertEquals(guest,    result.addedById)
        assertEquals(owner,    result.linkedUserId)
        assertEquals("Bière",  result.label)
        assertEquals(18.50,    result.amount, absoluteTolerance = 0.01)
    }

    @Test
    fun `addContribution - non-participant adder is denied`() = runTest {
        val owner    = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId  = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.addContribution(eventId, stranger, AddContributionDto(linkedUserId = owner, label = "x", amount = 1.0))
        }
    }

    @Test
    fun `addContribution - linked user not in event is denied`() = runTest {
        val owner    = createTestUser(email = "owner@test.com")
        val stranger = createTestUser(email = "stranger@test.com")
        val eventId  = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.addContribution(eventId, owner, AddContributionDto(linkedUserId = stranger, label = "x", amount = 1.0))
        }
    }

    @Test
    fun `addContribution - amount zero fails`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.addContribution(eventId, owner, AddContributionDto(linkedUserId = owner, label = "x", amount = 0.0))
        }
    }

    @Test
    fun `addContribution - blank label fails`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val eventId = createTestEvent(ownerId = owner)

        assertFailsWith<IllegalArgumentException> {
            service.addContribution(eventId, owner, AddContributionDto(linkedUserId = owner, label = "   ", amount = 5.0))
        }
    }

    // ── deleteContribution ─────────────────────────────────────────────────────

    @Test
    fun `deleteContribution - adder can delete their own`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val eventId = createTestEvent(ownerId = owner)

        val c = service.addContribution(eventId, owner, AddContributionDto(linkedUserId = owner, label = "Test", amount = 3.0))
        service.deleteContribution(eventId, c.id, owner)

        val list = service.getContributions(eventId, owner)
        assertTrue(list.none { it.id == c.id })
    }

    @Test
    fun `deleteContribution - non-adder is denied`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest)

        val c = service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Test", amount = 3.0))

        assertFailsWith<IllegalArgumentException> {
            service.deleteContribution(eventId, c.id, guest)
        }
    }

    // ── list ordering ──────────────────────────────────────────────────────────

    @Test
    fun `getContributions - list is sorted most recent first`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest)

        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "First", amount = 1.0))
        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Second", amount = 2.0))
        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Third", amount = 3.0))

        val list = service.getContributions(eventId, owner)
        assertEquals(3, list.size)
        // Most recent (highest id) first
        assertTrue(list[0].id > list[1].id)
        assertTrue(list[1].id > list[2].id)
    }

    // ── cascade on guest removal ───────────────────────────────────────────────

    @Test
    fun `removeGuest cascades to delete guest contributions`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val other   = createTestUser(email = "other@test.com")
        val eventId = createTestEvent(ownerId = owner)
        val inv     = invitationService.inviteByUserId(eventId, owner, guest)
        invitationService.inviteByUserId(eventId, owner, other)

        // Contribution added BY the guest
        service.addContribution(eventId, guest, AddContributionDto(linkedUserId = owner, label = "Glace", amount = 5.0))
        // Contribution added by owner, LINKED TO the guest
        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Chips", amount = 3.0))
        // Contribution unrelated to guest — should survive
        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = other, label = "Eau", amount = 2.0))

        val before = service.getContributions(eventId, owner)
        assertEquals(3, before.size)

        invitationService.removeGuest(eventId, inv.id, owner)

        val after = service.getContributions(eventId, owner)
        assertEquals(1, after.size)
        assertEquals("Eau", after[0].label)
    }

    @Test
    fun `rsvp DECLINED cascades to delete guest contributions`() = runTest {
        val owner   = createTestUser(email = "owner@test.com")
        val guest   = createTestUser(email = "guest@test.com")
        val eventId = createTestEvent(ownerId = owner)
        addInvitation(eventId, guest, InvitationStatus.ACCEPTED)

        // Contribution added by the guest, linked to owner
        service.addContribution(eventId, guest, AddContributionDto(linkedUserId = owner, label = "Glace", amount = 5.0))
        // Contribution added by owner, linked to guest
        service.addContribution(eventId, owner, AddContributionDto(linkedUserId = guest, label = "Chips", amount = 3.0))

        val token = transaction {
            com.partyplanner.db.tables.EventEntity.findById(eventId)!!.inviteToken!!
        }
        invitationService.rsvp(token, guest, "DECLINED")

        val rowCount = transaction {
            Contributions.selectAll().where { Contributions.eventId eq eventId }.count()
        }
        assertEquals(0L, rowCount)
    }
}
