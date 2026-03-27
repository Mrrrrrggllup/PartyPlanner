package com.partyplanner.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.db.tables.Users
import com.partyplanner.dto.AuthResponse
import com.partyplanner.dto.LoginRequest
import com.partyplanner.dto.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.util.Date

class AuthService(
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(request: RegisterRequest): AuthResponse = withContext(Dispatchers.IO) {
        logger.info("Register attempt for email=${request.email}")

        val existingUser = transaction {
            UserEntity.find { Users.email eq request.email }.firstOrNull()
        }
        require(existingUser == null) { "Email already in use: ${request.email}" }

        val user = transaction {
            UserEntity.new {
                email = request.email
                displayName = request.displayName
                phone = request.phone
                passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }

        logger.info("User registered successfully id=${user.id.value} email=${user.email}")
        AuthResponse(
            token = generateToken(user.id.value),
            userId = user.id.value,
            displayName = user.displayName
        )
    }

    suspend fun login(request: LoginRequest): AuthResponse = withContext(Dispatchers.IO) {
        logger.info("Login attempt for email=${request.email}")

        val user = transaction {
            UserEntity.find { Users.email eq request.email }.firstOrNull()
        } ?: run {
            logger.warn("Login failed: email not found email=${request.email}")
            error("Invalid email or password")
        }

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            logger.warn("Login failed: wrong password email=${request.email}")
            error("Invalid email or password")
        }

        logger.info("Login successful id=${user.id.value} email=${user.email}")
        AuthResponse(
            token = generateToken(user.id.value),
            userId = user.id.value,
            displayName = user.displayName
        )
    }

    fun verifyToken(token: String): Int? = runCatching {
        JWT.require(Algorithm.HMAC256(jwtSecret))
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .build()
            .verify(token)
            .getClaim("userId")
            .asInt()
    }.getOrNull()

    private fun generateToken(userId: Int): String =
        JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L))
            .sign(Algorithm.HMAC256(jwtSecret))
}