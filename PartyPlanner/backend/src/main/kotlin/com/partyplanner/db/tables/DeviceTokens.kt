package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object DeviceTokens : IntIdTable("device_tokens") {
    val userId    = reference("user_id", Users)
    val token     = varchar("token", 512).uniqueIndex()
    val platform  = varchar("platform", 10)
    val updatedAt = datetime("updated_at")
}

class DeviceTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeviceTokenEntity>(DeviceTokens)

    var user      by UserEntity referencedOn DeviceTokens.userId
    var token     by DeviceTokens.token
    var platform  by DeviceTokens.platform
    var updatedAt by DeviceTokens.updatedAt
}
