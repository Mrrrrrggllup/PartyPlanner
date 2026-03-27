package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.InvitationResponse
import com.partyplanner.data.remote.dto.InviteInfoResponse
import com.partyplanner.data.remote.dto.RsvpRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class InvitationApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private suspend fun bearerToken(): String {
        val token = sessionStorage.getSession()?.token ?: error("Not authenticated")
        return "Bearer $token"
    }

    suspend fun getInviteInfo(token: String): InviteInfoResponse {
        val response = httpClient.get("$baseUrl/invite/$token") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun rsvp(token: String, status: String): InviteInfoResponse {
        val response = httpClient.post("$baseUrl/invite/$token/rsvp") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(RsvpRequest(status))
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun getEventInvitations(eventId: Int): List<InvitationResponse> {
        val response = httpClient.get("$baseUrl/events/$eventId/invitations") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    private suspend fun errorMessage(response: HttpResponse): String = when (response.status) {
        HttpStatusCode.NotFound  -> "Invitation introuvable"
        HttpStatusCode.Forbidden -> "Accès refusé"
        HttpStatusCode.BadRequest -> runCatching {
            response.body<Map<String, String>>()["error"] ?: "Requête invalide"
        }.getOrDefault("Requête invalide")
        else -> "Erreur serveur (${response.status.value})"
    }
}
