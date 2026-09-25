package com.partyplanner.usecase.invitation

import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.usecase.invitation.GetEventInvitationsUseCase
import com.partyplanner.domain.usecase.invitation.GetInviteSuggestionsUseCase
import com.partyplanner.domain.usecase.invitation.GetInviteInfoUseCase
import com.partyplanner.domain.usecase.invitation.InviteByEmailUseCase
import com.partyplanner.domain.usecase.invitation.InviteByUserIdUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import com.partyplanner.fake.FakeInvitationRepository
import com.partyplanner.fake.testInvitation
import com.partyplanner.fake.testInviteInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetInviteInfoUseCaseTest {

    @Test
    fun `returns invite info on success`() = runTest {
        val result = GetInviteInfoUseCase(FakeInvitationRepository())("token-abc")
        assertTrue(result.isSuccess)
        assertEquals(testInviteInfo, result.getOrNull())
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = GetInviteInfoUseCase(
            FakeInvitationRepository(getInviteInfoResult = Result.failure(Exception("Invitation introuvable")))
        )("bad-token")
        assertTrue(result.isFailure)
        assertEquals("Invitation introuvable", result.exceptionOrNull()?.message)
    }
}

class RsvpToInvitationUseCaseTest {

    @Test
    fun `delegates token and status to repository`() = runTest {
        val fake = FakeInvitationRepository()
        RsvpToInvitationUseCase(fake)("token-abc", InvitationStatus.ACCEPTED)
        assertEquals("token-abc", fake.lastRsvpToken)
        assertEquals(InvitationStatus.ACCEPTED, fake.lastRsvpStatus)
    }

    @Test
    fun `returns updated invite info on success`() = runTest {
        val result = RsvpToInvitationUseCase(FakeInvitationRepository())("token", InvitationStatus.ACCEPTED)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = RsvpToInvitationUseCase(
            FakeInvitationRepository(rsvpResult = Result.failure(Exception("Invitation introuvable")))
        )("bad-token", InvitationStatus.DECLINED)
        assertTrue(result.isFailure)
    }
}

class GetEventInvitationsUseCaseTest {

    @Test
    fun `returns invitation list on success`() = runTest {
        val result = GetEventInvitationsUseCase(
            FakeInvitationRepository(getEventInvitationsResult = Result.success(listOf(testInvitation)))
        )(eventId = 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `returns empty list when no invitations`() = runTest {
        val result = GetEventInvitationsUseCase(FakeInvitationRepository())(1)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
}

class InviteByEmailUseCaseTest {

    @Test
    fun `returns created invitation on success`() = runTest {
        val result = InviteByEmailUseCase(
            FakeInvitationRepository(inviteByEmailResult = Result.success(testInvitation))
        )(eventId = 1, email = "bob@example.com")
        assertTrue(result.isSuccess)
        assertEquals(testInvitation, result.getOrNull())
    }

    @Test
    fun `propagates already-invited failure`() = runTest {
        val result = InviteByEmailUseCase(
            FakeInvitationRepository(inviteByEmailResult = Result.failure(Exception("Utilisateur déjà invité")))
        )(1, "bob@example.com")
        assertTrue(result.isFailure)
        assertEquals("Utilisateur déjà invité", result.exceptionOrNull()?.message)
    }
}

class InviteByUserIdUseCaseTest {

    @Test
    fun `returns created invitation on success`() = runTest {
        val result = InviteByUserIdUseCase(
            FakeInvitationRepository(inviteByUserIdResult = Result.success(testInvitation))
        )(eventId = 1, userId = 2)
        assertTrue(result.isSuccess)
        assertEquals(testInvitation, result.getOrNull())
    }

    @Test
    fun `propagates self-invite failure`() = runTest {
        val result = InviteByUserIdUseCase(
            FakeInvitationRepository(inviteByUserIdResult = Result.failure(Exception("Vous ne pouvez pas vous inviter vous-même")))
        )(1, 1)
        assertTrue(result.isFailure)
    }
}

class GetInviteSuggestionsUseCaseTest {

    @Test
    fun `returns suggestions on success`() = runTest {
        val suggestions = listOf(com.partyplanner.domain.model.UserSuggestion(3, "Charlie"))
        val result = GetInviteSuggestionsUseCase(
            FakeInvitationRepository(getSuggestionsResult = Result.success(suggestions))
        )(eventId = 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `returns empty list when no suggestions`() = runTest {
        val result = GetInviteSuggestionsUseCase(FakeInvitationRepository())(1)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
}
