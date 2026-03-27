package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.ChatMessageResponse
import com.partyplanner.data.remote.dto.SendChatMessageDto
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private var currentSession: DefaultClientWebSocketSession? = null

    // Suspends until the WS connection is closed — call inside a background coroutine
    suspend fun connect(eventId: Int, onMessage: (ChatMessageResponse) -> Unit) {
        val token = sessionStorage.getSession()?.token ?: error("Not authenticated")
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        httpClient.webSocket("$wsUrl/events/$eventId/chat?token=$token") {
            currentSession = this
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        runCatching {
                            Json.decodeFromString<ChatMessageResponse>(frame.readText())
                        }.onSuccess { onMessage(it) }
                    }
                }
            } finally {
                currentSession = null
            }
        }
    }

    suspend fun send(content: String) {
        currentSession?.send(
            Frame.Text(Json.encodeToString(SendChatMessageDto(content)))
        )
    }
}
