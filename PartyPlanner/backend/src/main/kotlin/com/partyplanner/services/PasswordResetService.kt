package com.partyplanner.services

import com.partyplanner.db.tables.PasswordResetTokenEntity
import com.partyplanner.db.tables.PasswordResetTokens
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.db.tables.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.time.Duration.Companion.hours

class PasswordResetService(
    private val resendApiKey: String,
    private val fromEmail: String,
    private val appBaseUrl: String,
) {
    private val http = HttpClient.newHttpClient()

    suspend fun requestReset(email: String): Unit = withContext(Dispatchers.IO) {
        val user = transaction {
            UserEntity.find { Users.email eq email }.firstOrNull()
        } ?: return@withContext // Silent — don't reveal if email exists

        val token     = UUID.randomUUID().toString().replace("-", "")
        val expiresAt = (Clock.System.now() + 1.hours).toLocalDateTime(TimeZone.UTC)

        transaction {
            PasswordResetTokenEntity.new {
                this.user      = user
                this.token     = token
                this.expiresAt = expiresAt
            }
        }

        if (resendApiKey.isNotBlank()) {
            sendResetEmail(user.email, user.displayName, token)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Unit = withContext(Dispatchers.IO) {
        require(newPassword.length >= 8) { "Le mot de passe doit faire au moins 8 caractères" }

        transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entry = PasswordResetTokenEntity.find {
                PasswordResetTokens.token eq token
            }.firstOrNull() ?: error("Lien invalide ou expiré")

            require(entry.usedAt == null) { "Ce lien a déjà été utilisé" }
            require(entry.expiresAt > now) { "Ce lien a expiré" }

            entry.user.passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
            entry.usedAt = now
        }
    }

    private fun sendResetEmail(toEmail: String, displayName: String, token: String) {
        val resetLink = "$appBaseUrl?token=$token"
        val bodyJson = """
            {
              "from": "$fromEmail",
              "to": ["$toEmail"],
              "subject": "Réinitialisation de ton mot de passe PartyPlanner",
              "html": "<p>Bonjour $displayName,</p><p>Clique sur le lien pour réinitialiser ton mot de passe (valable 1h) :</p><p><a href=\"$resetLink\">Réinitialiser mon mot de passe</a></p><p>Si tu n'as pas fait cette demande, ignore cet email.</p>"
            }
        """.trimIndent()

        runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer $resendApiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build()
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }.onFailure { println("Failed to send reset email: $it") }
    }
}
