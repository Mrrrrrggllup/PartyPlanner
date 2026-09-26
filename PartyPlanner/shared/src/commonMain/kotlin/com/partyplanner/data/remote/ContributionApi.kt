package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.AddContributionRequest
import com.partyplanner.data.remote.dto.ContributionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ContributionApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private suspend fun bearerToken(): String {
        val token = sessionStorage.getSession()?.token ?: error("Not authenticated")
        return "Bearer $token"
    }

    suspend fun getContributions(eventId: Int): List<ContributionResponse> {
        val response = httpClient.get("$baseUrl/events/$eventId/contributions") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun addContribution(eventId: Int, linkedUserId: Int, label: String, amount: Double): ContributionResponse {
        val response = httpClient.post("$baseUrl/events/$eventId/contributions") {
            header(HttpHeaders.Authorization, bearerToken())
            contentType(ContentType.Application.Json)
            setBody(AddContributionRequest(linkedUserId, label, amount))
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
        return response.body()
    }

    suspend fun deleteContribution(eventId: Int, contributionId: Int) {
        val response = httpClient.delete("$baseUrl/events/$eventId/contributions/$contributionId") {
            header(HttpHeaders.Authorization, bearerToken())
        }
        if (!response.status.isSuccess()) throw Exception(errorMessage(response))
    }

    private suspend fun errorMessage(response: HttpResponse): String = when (response.status) {
        HttpStatusCode.Forbidden -> "Accès refusé"
        HttpStatusCode.BadRequest -> runCatching {
            response.body<Map<String, String>>()["error"] ?: "Requête invalide"
        }.getOrDefault("Requête invalide")
        else -> "Erreur serveur (${response.status.value})"
    }
}
