package com.partyplanner.routes

import com.partyplanner.dto.SendChatMessageDto
import com.partyplanner.services.AuthService
import com.partyplanner.services.ChatService
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.chatRoutes(chatService: ChatService, authService: AuthService) {
    webSocket("/events/{id}/chat") {
        val eventId = call.parameters["id"]?.toIntOrNull()
            ?: return@webSocket close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Invalid event id"
                )
            )

        val token = call.request.queryParameters["token"]
            ?: return@webSocket close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Missing token"
                )
            )

        val userId = authService.verifyToken(token)
            ?: return@webSocket close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Invalid token"
                )
            )

        if (!chatService.checkAccess(eventId, userId)) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Access denied"))
            return@webSocket
        }

        chatService.addSession(eventId, this)
        try {
            // Send history on connect
            chatService.getHistory(eventId).forEach { msg ->
                send(Frame.Text(Json.encodeToString(msg)))
            }

            // Receive messages
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val dto = runCatching {
                        Json.decodeFromString<SendChatMessageDto>(frame.readText())
                    }.getOrNull() ?: continue

                    if (dto.content.isBlank()) continue

                    val saved = chatService.saveMessage(eventId, userId, dto.content.trim())
                    chatService.broadcast(eventId, saved)
                }
            }
        } finally {
            chatService.removeSession(eventId, this)
        }
    }
}
