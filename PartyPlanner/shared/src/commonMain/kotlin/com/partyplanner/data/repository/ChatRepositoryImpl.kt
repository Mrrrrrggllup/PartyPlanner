package com.partyplanner.data.repository

import com.partyplanner.data.remote.ChatApi
import com.partyplanner.data.remote.dto.ChatMessageResponse
import com.partyplanner.domain.model.ChatMessage
import com.partyplanner.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.LocalDateTime

class ChatRepositoryImpl(private val api: ChatApi) : ChatRepository {

    private val _messages = MutableSharedFlow<ChatMessage>(replay = 0, extraBufferCapacity = 128)
    override val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()

    override suspend fun connect(eventId: Int) {
        api.connect(eventId) { response ->
            _messages.tryEmit(response.toDomain())
        }
    }

    override suspend fun send(content: String) {
        api.send(content)
    }

    private fun ChatMessageResponse.toDomain() = ChatMessage(
        id         = id,
        senderId   = senderId,
        senderName = senderName,
        content    = content,
        createdAt  = createdAt,
    )
}
