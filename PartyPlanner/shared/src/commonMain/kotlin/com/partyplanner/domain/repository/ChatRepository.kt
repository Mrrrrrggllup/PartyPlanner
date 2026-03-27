package com.partyplanner.domain.repository

import com.partyplanner.domain.model.ChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatRepository {
    val messages: SharedFlow<ChatMessage>
    // Suspends until connection closes — run in a background coroutine
    suspend fun connect(eventId: Int)
    suspend fun send(content: String)
}
