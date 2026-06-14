package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object PasswordResetTokens : IntIdTable("password_reset_tokens") {
    val userId    = reference("user_id", Users)
    val token     = varchar("token", 100).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val usedAt    = datetime("used_at").nullable()
}

class PasswordResetTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PasswordResetTokenEntity>(PasswordResetTokens)

    var user      by UserEntity referencedOn PasswordResetTokens.userId
    var token     by PasswordResetTokens.token
    var expiresAt by PasswordResetTokens.expiresAt
    var usedAt    by PasswordResetTokens.usedAt
}
