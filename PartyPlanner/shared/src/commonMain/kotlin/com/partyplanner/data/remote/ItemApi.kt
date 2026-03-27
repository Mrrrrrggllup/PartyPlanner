package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ItemApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private suspend fun bearerToken(): String {
        val token = sessionStorage.getSession()?.token ?: error("Not authenticated")
        return "Bearer $token"
    }

    suspend fun getCategories(): List<CategoryResponse> {
        val response = httpClient.get("$baseUrl/items/categories") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun getItems(eventId: Int): ItemsResponse {
        val response = httpClient.get("$baseUrl/events/$eventId/items") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun addItemRequest(eventId: Int, dto: AddItemRequestDto): ItemRequestResponse {
        val response = httpClient.post("$baseUrl/events/$eventId/items/requests") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun fulfillRequest(eventId: Int, requestId: Int): ItemRequestResponse {
        val response = httpClient.post("$baseUrl/events/$eventId/items/requests/$requestId/fulfill") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun deleteItemRequest(eventId: Int, requestId: Int) {
        val response = httpClient.delete("$baseUrl/events/$eventId/items/requests/$requestId") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
    }

    suspend fun addItemBrought(eventId: Int, dto: AddItemBroughtDto): ItemBroughtResponse {
        val response = httpClient.post("$baseUrl/events/$eventId/items/brought") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun deleteItemBrought(eventId: Int, broughtId: Int) {
        val response = httpClient.delete("$baseUrl/events/$eventId/items/brought/$broughtId") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
    }

    private suspend fun errorMessage(response: HttpResponse): String = when (response.status) {
        HttpStatusCode.NotFound  -> "Introuvable"
        HttpStatusCode.Forbidden -> "Accès refusé"
        else -> "Erreur serveur (${response.status.value})"
    }
}
