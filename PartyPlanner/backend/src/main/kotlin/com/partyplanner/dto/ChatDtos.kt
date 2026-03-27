package com.partyplanner.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SendChatMessageDto(
    val content: String,
)

@Serializable
data class ChatMessageResponse(
    val id: Int,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val createdAt: LocalDateTime,
)
