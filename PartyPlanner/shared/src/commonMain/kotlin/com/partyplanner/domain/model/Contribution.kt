package com.partyplanner.domain.model

import kotlinx.datetime.LocalDateTime

data class Contribution(
    val id: Int,
    val addedById: Int,
    val linkedUserId: Int,
    val linkedUserName: String,
    val label: String,
    val amount: Double,
    val createdAt: LocalDateTime,
)
