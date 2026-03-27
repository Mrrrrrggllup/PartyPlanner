package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Events : IntIdTable("events") {
    val title       = varchar("title", 200)
    val description = text("description").nullable()
    val location    = varchar("location", 500).nullable()
    val latitude    = double("latitude").nullable()
    val longitude   = double("longitude").nullable()
    val startDate   = datetime("start_date")
    val endDate     = datetime("end_date").nullable()
    val ownerId     = reference("owner_id", Users)
    val inviteToken = varchar("invite_token", 36).nullable().uniqueIndex()
    val createdAt   = datetime("created_at")
}

class EventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EventEntity>(Events)

    var title       by Events.title
    var description by Events.description
    var location    by Events.location
    var latitude    by Events.latitude
    var longitude   by Events.longitude
    var startDate   by Events.startDate
    var endDate     by Events.endDate
    var owner       by UserEntity referencedOn Events.ownerId
    var inviteToken by Events.inviteToken
    var createdAt   by Events.createdAt
}
