package com.partyplanner.domain.model

import kotlinx.datetime.LocalDateTime

data class ChatMessage(
    val id: Int,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val createdAt: LocalDateTime,
)
