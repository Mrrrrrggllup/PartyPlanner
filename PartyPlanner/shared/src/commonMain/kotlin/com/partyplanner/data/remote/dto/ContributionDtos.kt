package com.partyplanner.data.remote.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class AddContributionRequest(
    val linkedUserId: Int,
    val label: String,
    val amount: Double,
)

@Serializable
data class ContributionResponse(
    val id: Int,
    val addedById: Int,
    val linkedUserId: Int,
    val linkedUserName: String,
    val label: String,
    val amount: Double,
    val createdAt: LocalDateTime,
)
