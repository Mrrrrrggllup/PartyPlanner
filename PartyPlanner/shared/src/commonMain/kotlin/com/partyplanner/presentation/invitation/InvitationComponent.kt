package com.partyplanner.presentation.invitation

import com.partyplanner.domain.model.InvitationStatus
import kotlinx.coroutines.flow.StateFlow

interface InvitationComponent {
    val state: StateFlow<InvitationState>
    fun onRsvp(status: InvitationStatus)
    fun onBack()
}
