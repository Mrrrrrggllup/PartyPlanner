package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.CreateEventRequest
import com.partyplanner.data.remote.dto.EventResponse
import com.partyplanner.data.remote.dto.UpdateEventRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class EventApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private suspend fun bearerToken(): String {
        val token = sessionStorage.getSession()?.token ?: error("Not authenticated")
        return "Bearer $token"
    }

    suspend fun getEvents(): List<EventResponse> {
        val response = httpClient.get("$baseUrl/events") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response.status))
        return response.body()
    }

    suspend fun getEvent(id: Int): EventResponse {
        val response = httpClient.get("$baseUrl/events/$id") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response.status))
        return response.body()
    }

    suspend fun createEvent(request: CreateEventRequest): EventResponse {
        val response = httpClient.post("$baseUrl/events") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response.status))
        return response.body()
    }

    suspend fun updateEvent(id: Int, request: UpdateEventRequest): EventResponse {
        val response = httpClient.put("$baseUrl/events/$id") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response.status))
        return response.body()
    }

    suspend fun deleteEvent(id: Int) {
        val response = httpClient.delete("$baseUrl/events/$id") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response.status))
    }

    private fun errorMessage(status: HttpStatusCode): String = when (status) {
        HttpStatusCode.NotFound  -> "Événement introuvable"
        HttpStatusCode.Forbidden -> "Accès refusé"
        else -> "Erreur serveur (${status.value})"
    }
}
