package com.partyplanner.data.remote

import com.partyplanner.data.remote.dto.AuthResponse
import com.partyplanner.data.remote.dto.LoginRequest
import com.partyplanner.data.remote.dto.RegisterRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class AuthApi(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    suspend fun register(request: RegisterRequest): AuthResponse {
        val response = httpClient.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception(errorMessage(response.status))
        }
        return response.body()
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val response = httpClient.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception(errorMessage(response.status))
        }
        return response.body()
    }

    private fun errorMessage(status: HttpStatusCode): String = when (status) {
        HttpStatusCode.Unauthorized -> "Email ou mot de passe incorrect"
        HttpStatusCode.Conflict -> "Un compte existe déjà avec cet email"
        else -> "Erreur serveur (${status.value})"
    }
}