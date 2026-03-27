package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object ItemCategories : IntIdTable("item_categories") {
    val label    = varchar("label", 100)
    val icon     = varchar("icon", 10).nullable()
    val parentId = reference("parent_id", ItemCategories).nullable()
}

class ItemCategoryEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ItemCategoryEntity>(ItemCategories)

    var label    by ItemCategories.label
    var icon     by ItemCategories.icon
    var parentId by ItemCategories.parentId
}
