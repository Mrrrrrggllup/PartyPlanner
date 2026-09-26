package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Contributions : IntIdTable("contributions") {
    val eventId      = reference("event_id", Events)
    val addedById    = reference("added_by_id", Users)
    val linkedUserId = reference("linked_user_id", Users)
    val label        = varchar("label", 200)
    val amount       = decimal("amount", precision = 10, scale = 2)
    val createdAt    = datetime("created_at")
}

class ContributionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ContributionEntity>(Contributions)

    var event      by EventEntity referencedOn Contributions.eventId
    var addedBy    by UserEntity  referencedOn Contributions.addedById
    var linkedUser by UserEntity  referencedOn Contributions.linkedUserId
    var label      by Contributions.label
    var amount     by Contributions.amount
    var createdAt  by Contributions.createdAt
}
