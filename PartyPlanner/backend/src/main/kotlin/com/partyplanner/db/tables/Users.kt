package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Users : IntIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 20).nullable()
    val displayName = varchar("display_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(Users)

    var email by Users.email
    var phone by Users.phone
    var displayName by Users.displayName
    var passwordHash by Users.passwordHash
    var createdAt by Users.createdAt
}