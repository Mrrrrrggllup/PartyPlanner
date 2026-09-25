package com.partyplanner.data.remote

import com.partyplanner.data.local.SessionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionStorage: SessionStorage,
) {
    private fun token(): String? = sessionStorage.getSession()?.token

    suspend fun registerDeviceToken(fcmToken: String) {
        val authToken = token() ?: return
        runCatching {
            httpClient.post("$baseUrl/users/me/device-token") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to fcmToken, "platform" to "android"))
            }
        }
    }

    suspend fun markActive() {
        val authToken = token() ?: return
        runCatching {
            httpClient.delete("$baseUrl/users/me/notification") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
    }
}
