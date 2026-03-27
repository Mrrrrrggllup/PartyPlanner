package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.dto.CarpoolOfferResponse
import com.partyplanner.data.remote.dto.CreateCarpoolOfferDto
import com.partyplanner.data.remote.dto.JoinCarpoolDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class CarpoolApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private suspend fun bearer() =
        "Bearer ${sessionStorage.getSession()?.token ?: error("Not authenticated")}"

    suspend fun getOffers(eventId: Int): List<CarpoolOfferResponse> {
        val r = httpClient.get("$baseUrl/events/$eventId/carpool") { header(HttpHeaders.Authorization, bearer()) }
        if (!r.status.isSuccess()) throw Exception(errorMessage(r))
        return r.body()
    }

    suspend fun createOffer(eventId: Int, dto: CreateCarpoolOfferDto): CarpoolOfferResponse {
        val r = httpClient.post("$baseUrl/events/$eventId/carpool") {
            header(HttpHeaders.Authorization, bearer())
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        if (!r.status.isSuccess()) throw Exception(errorMessage(r))
        return r.body()
    }

    suspend fun deleteOffer(eventId: Int, offerId: Int) {
        val r = httpClient.delete("$baseUrl/events/$eventId/carpool/$offerId") {
            header(HttpHeaders.Authorization, bearer())
        }
        if (!r.status.isSuccess()) throw Exception(errorMessage(r))
    }

    suspend fun joinOffer(eventId: Int, offerId: Int, pickupPoint: String?): CarpoolOfferResponse {
        val r = httpClient.post("$baseUrl/events/$eventId/carpool/$offerId/join") {
            header(HttpHeaders.Authorization, bearer())
            contentType(ContentType.Application.Json)
            setBody(JoinCarpoolDto(pickupPoint))
        }
        if (!r.status.isSuccess()) throw Exception(errorMessage(r))
        return r.body()
    }

    suspend fun leaveOffer(eventId: Int, offerId: Int): CarpoolOfferResponse {
        val r = httpClient.post("$baseUrl/events/$eventId/carpool/$offerId/leave") {
            header(HttpHeaders.Authorization, bearer())
        }
        if (!r.status.isSuccess()) throw Exception(errorMessage(r))
        return r.body()
    }

    private suspend fun errorMessage(r: HttpResponse) = when (r.status) {
        HttpStatusCode.Forbidden  -> "Accès refusé"
        HttpStatusCode.BadRequest -> runCatching { r.body<Map<String, String>>()["error"] ?: "Requête invalide" }.getOrDefault("Requête invalide")
        else -> "Erreur serveur (${r.status.value})"
    }
}
