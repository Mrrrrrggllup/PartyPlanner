package com.partyplanner.presentation.invitation

import com.partyplanner.domain.model.InviteInfo

sealed class InvitationState {
    data object Loading : InvitationState()
    data class Success(val info: InviteInfo, val isSubmitting: Boolean = false) : InvitationState()
    data class Error(val message: String) : InvitationState()
}
