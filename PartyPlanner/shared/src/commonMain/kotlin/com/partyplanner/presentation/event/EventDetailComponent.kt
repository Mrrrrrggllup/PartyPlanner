package com.partyplanner.presentation.event

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
    fun onCreateCarpoolOffer(seats: Int, departurePoint: String?, notes: String?)
    fun onDeleteCarpoolOffer(offerId: Int)
    fun onJoinCarpool(offerId: Int, pickupPoint: String?)
    fun onLeaveCarpool(offerId: Int)
    fun onSendMessage(content: String)
}
