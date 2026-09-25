package com.partyplanner

import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.PasswordResetTokenEntity
import com.partyplanner.db.tables.UserEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

fun createTestUser(
    email: String = "user_${System.nanoTime()}@test.com",
    displayName: String = "Test User",
    password: String = "password",
): Int = transaction {
    UserEntity.new {
        this.email = email
        this.displayName = displayName
        this.phone = null
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }.id.value
}

fun createTestEvent(
    ownerId: Int,
    title: String = "Test Event",
): Int = transaction {
    val owner = UserEntity.findById(ownerId)!!
    EventEntity.new {
        this.title = title
        this.description = null
        this.location = null
        this.startDate = LocalDateTime(2026, 10, 1, 18, 0)
        this.endDate = null
        this.owner = owner
        this.inviteToken = UUID.randomUUID().toString()
        this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }.id.value
}

fun addInvitation(
    eventId: Int,
    userId: Int,
    status: InvitationStatus = InvitationStatus.ACCEPTED,
): Unit = transaction {
    InvitationEntity.new {
        this.event = EventEntity.findById(eventId)!!
        this.user = UserEntity.findById(userId)!!
        this.status = status
    }
}

fun createResetToken(
    userId: Int,
    expiresIn: Duration = 1.hours,
): String = transaction {
    val token = UUID.randomUUID().toString().replace("-", "")
    PasswordResetTokenEntity.new {
        this.user = UserEntity.findById(userId)!!
        this.token = token
        this.expiresAt = (Clock.System.now() + expiresIn).toLocalDateTime(TimeZone.UTC)
    }
    token
}
