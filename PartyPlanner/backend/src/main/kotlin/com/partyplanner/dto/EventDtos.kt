package com.partyplanner.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime? = null,
)

@Serializable
data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
)

@Serializable
data class EventResponse(
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
