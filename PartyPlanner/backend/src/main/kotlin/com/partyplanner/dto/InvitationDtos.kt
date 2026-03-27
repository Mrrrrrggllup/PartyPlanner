package com.partyplanner.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InviteInfoResponse(
    val eventId: Int,
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val location: String?,
    val organizerName: String,
    val isOwner: Boolean,
    val currentStatus: String?,   // null = not yet responded
)

@Serializable
data class InvitationResponse(
    val id: Int,
    val userId: Int,
    val userDisplayName: String,
    val status: String,
    val respondedAt: LocalDateTime?,
)

@Serializable
data class RsvpRequest(
    val status: String,  // ACCEPTED | DECLINED | MAYBE
)
