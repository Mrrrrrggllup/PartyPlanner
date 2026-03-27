package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import java.math.BigDecimal

object ItemRequests : IntIdTable("item_requests") {
    val eventId     = reference("event_id", Events)
    val label       = varchar("label", 200)
    val quantity    = integer("quantity").default(1)
    val categoryId  = reference("category_id", ItemCategories).nullable()
    val assignedTo  = reference("assigned_to", Users).nullable()
    val isFulfilled = bool("is_fulfilled").default(false)
    /** Reserved for future pot commun — not exposed in API yet. */
    val price       = decimal("price", precision = 10, scale = 2).nullable()
}

class ItemRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ItemRequestEntity>(ItemRequests)

    var event       by EventEntity          referencedOn         ItemRequests.eventId
    var label       by ItemRequests.label
    var quantity    by ItemRequests.quantity
    var category    by ItemCategoryEntity   optionalReferencedOn ItemRequests.categoryId
    var assignedTo  by UserEntity           optionalReferencedOn ItemRequests.assignedTo
    var isFulfilled by ItemRequests.isFulfilled
    var price       by ItemRequests.price
}

object ItemsBrought : IntIdTable("items_brought") {
    val eventId    = reference("event_id", Events)
    val userId     = reference("user_id", Users)
    val label      = varchar("label", 200)
    val quantity   = integer("quantity").default(1)
    val categoryId = reference("category_id", ItemCategories).nullable()
}

class ItemBroughtEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ItemBroughtEntity>(ItemsBrought)

    var event    by EventEntity          referencedOn         ItemsBrought.eventId
    var user     by UserEntity           referencedOn         ItemsBrought.userId
    var label    by ItemsBrought.label
    var quantity by ItemsBrought.quantity
    var category by ItemCategoryEntity   optionalReferencedOn ItemsBrought.categoryId
}
