package com.partyplanner.presentation.event

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.ChatMessage
import com.partyplanner.domain.model.Event
import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.model.UserSuggestion

sealed class InviteEmailResult {
    data class Success(val userName: String) : InviteEmailResult()
    data class Error(val message: String) : InviteEmailResult()
}

sealed class EventDetailState {
    data object Loading : EventDetailState()
    data class Success(
        val event: Event,
        val isOwner: Boolean,
        val invitations: List<Invitation> = emptyList(),
        val items: EventItems = EventItems(emptyList(), emptyList()),
        val categories: List<ItemCategory> = emptyList(),
        val currentUserId: Int = 0,
        val carpoolOffers: List<CarpoolOffer> = emptyList(),
        val chatMessages: List<ChatMessage> = emptyList(),
        val currentUserInvitationStatus: InvitationStatus? = null,
        val inviteEmailResult: InviteEmailResult? = null,
        val inviteSuggestions: List<UserSuggestion> = emptyList(),
    ) : EventDetailState()
    data class Error(val message: String) : EventDetailState()
}
