package com.partyplanner.domain.model

import kotlinx.datetime.LocalDateTime

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, MAYBE }

data class InviteInfo(
    val eventId: Int,
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val location: String?,
    val organizerName: String,
    val isOwner: Boolean,
    val currentStatus: InvitationStatus?,
)

data class Invitation(
    val id: Int,
    val userId: Int,
    val userDisplayName: String,
    val status: InvitationStatus,
    val respondedAt: LocalDateTime?,
)
