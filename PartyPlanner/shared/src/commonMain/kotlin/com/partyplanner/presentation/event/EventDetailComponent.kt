package com.partyplanner.presentation.event

import com.partyplanner.domain.model.InvitationStatus
import kotlinx.coroutines.flow.StateFlow

interface EventDetailComponent {
    val state: StateFlow<EventDetailState>
    fun onBack()
    fun onDelete()
    fun onAddItemRequest(label: String, quantity: Int, categoryId: Int?)
    fun onFulfillItemRequest(requestId: Int)
    fun onDeleteItemRequest(requestId: Int)
    fun onAddItemBrought(label: String, quantity: Int, categoryId: Int?)
    fun onDeleteItemBrought(broughtId: Int)
    fun onItemsRead()
    fun onCreateCarpoolOffer(seats: Int, departurePoint: String?, notes: String?)
    fun onUpdateCarpoolOffer(offerId: Int, seats: Int, departurePoint: String?, notes: String?)
    fun onDeleteCarpoolOffer(offerId: Int)
    fun onJoinCarpool(offerId: Int, pickupPoint: String?)
    fun onLeaveCarpool(offerId: Int)
    fun onCarpoolRead()
    fun onSendMessage(content: String)
    fun onInviteByEmail(email: String)
    fun onInviteByUserId(userId: Int)
    fun onDismissInviteResult()
    fun onDismissDeleteError()
    fun onRsvp(status: InvitationStatus)
    fun onRefresh()
    fun onChatRead()
    fun onChatLeft()
    fun onEdit()
}
