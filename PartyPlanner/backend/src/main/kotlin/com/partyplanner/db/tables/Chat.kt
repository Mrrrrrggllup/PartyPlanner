package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ChatMessages : IntIdTable("chat_messages") {
    val eventId   = reference("event_id", Events)
    val senderId  = reference("sender_id", Users)
    val content   = text("content")
    val createdAt = datetime("created_at")
}

class ChatMessageEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ChatMessageEntity>(ChatMessages)

    var event     by EventEntity referencedOn ChatMessages.eventId
    var sender    by UserEntity  referencedOn ChatMessages.senderId
    var content   by ChatMessages.content
    var createdAt by ChatMessages.createdAt
}
