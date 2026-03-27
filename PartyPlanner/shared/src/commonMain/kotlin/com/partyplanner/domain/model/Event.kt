package com.partyplanner.domain.model

import kotlinx.datetime.LocalDateTime

data class Event(
    val id: Int,
    val title: String,
    val description: String?,
    val location: String?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val ownerId: Int,
    val inviteToken: String?,
    val createdAt: LocalDateTime,
)
