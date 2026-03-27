package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, MAYBE }

object Invitations : IntIdTable("invitations") {
    val eventId     = reference("event_id", Events)
    val userId      = reference("user_id", Users)
    val status      = enumerationByName("status", 20, InvitationStatus::class)
    val respondedAt = datetime("responded_at").nullable()

    init {
        uniqueIndex(eventId, userId)
    }
}

class InvitationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InvitationEntity>(Invitations)

    var event       by EventEntity referencedOn Invitations.eventId
    var user        by UserEntity  referencedOn Invitations.userId
    var status      by Invitations.status
    var respondedAt by Invitations.respondedAt
}
