package com.partyplanner.services

import com.partyplanner.db.tables.*
import com.partyplanner.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class ItemService {

    // ── Access helpers ────────────────────────────────────────────────────────

    private fun checkAccess(eventId: Int, userId: Int) {
        val event = EventEntity.findById(eventId) ?: error("Event not found")
        val isOwner   = event.owner.id.value == userId
        val isInvited = InvitationEntity.find {
            (Invitations.eventId eq eventId) and (Invitations.userId eq userId)
        }.firstOrNull() != null
        require(isOwner || isInvited) { "Access denied" }
    }

    private fun checkOwner(eventId: Int, userId: Int) {
        val event = EventEntity.findById(eventId) ?: error("Event not found")
        require(event.owner.id.value == userId) { "Owner only" }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    suspend fun getCategories(): List<CategoryResponse> = withContext(Dispatchers.IO) {
        transaction {
            ItemCategoryEntity.all()
                .sortedBy { it.id.value }
                .map { CategoryResponse(it.id.value, it.label, it.icon) }
        }
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    suspend fun getItems(eventId: Int, userId: Int): ItemsResponse = withContext(Dispatchers.IO) {
        transaction {
            checkAccess(eventId, userId)
            val requests = ItemRequestEntity
                .find { ItemRequests.eventId eq eventId }
                .orderBy(ItemRequests.categoryId to SortOrder.ASC_NULLS_LAST, ItemRequests.id to SortOrder.ASC)
                .map { it.toResponse() }
            val brought = ItemBroughtEntity
                .find { ItemsBrought.eventId eq eventId }
                .orderBy(ItemsBrought.categoryId to SortOrder.ASC_NULLS_LAST, ItemsBrought.id to SortOrder.ASC)
                .map { it.toResponse() }
            ItemsResponse(requests, brought)
        }
    }

    // ── ItemRequest ───────────────────────────────────────────────────────────

    /** Any participant (owner or invited) can suggest an item. */
    suspend fun addItemRequest(eventId: Int, userId: Int, dto: AddItemRequestDto): ItemRequestResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                val event    = EventEntity.findById(eventId)!!
                val category = dto.categoryId?.let { ItemCategoryEntity.findById(it) }
                ItemRequestEntity.new {
                    this.event    = event
                    label         = dto.label.trim()
                    quantity      = dto.quantity.coerceAtLeast(1)
                    this.category = category
                    isFulfilled   = false
                }.toResponse()
            }
        }

    /** Toggle fulfilled. Any participant can volunteer or unvolunteer. */
    suspend fun fulfillRequest(eventId: Int, requestId: Int, userId: Int): ItemRequestResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                val req = ItemRequestEntity.findById(requestId) ?: error("Item not found")
                require(req.event.id.value == eventId) { "Item not found" }
                val nowFulfilled = !req.isFulfilled
                req.isFulfilled = nowFulfilled
                req.assignedTo  = if (nowFulfilled) UserEntity.findById(userId) else null
                req.toResponse()
            }
        }

    /** Only owner can delete item requests. */
    suspend fun deleteItemRequest(eventId: Int, requestId: Int, userId: Int): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                checkOwner(eventId, userId)
                val req = ItemRequestEntity.findById(requestId) ?: error("Item not found")
                require(req.event.id.value == eventId) { "Item not found" }
                req.delete()
            }
        }

    // ── ItemBrought ───────────────────────────────────────────────────────────

    suspend fun addItemBrought(eventId: Int, userId: Int, dto: AddItemBroughtDto): ItemBroughtResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                val event    = EventEntity.findById(eventId)!!
                val user     = UserEntity.findById(userId)!!
                val category = dto.categoryId?.let { ItemCategoryEntity.findById(it) }
                ItemBroughtEntity.new {
                    this.event    = event
                    this.user     = user
                    label         = dto.label.trim()
                    quantity      = dto.quantity.coerceAtLeast(1)
                    this.category = category
                }.toResponse()
            }
        }

    /** Owner can delete any; author can delete their own. */
    suspend fun deleteItemBrought(eventId: Int, broughtId: Int, userId: Int): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                val item = ItemBroughtEntity.findById(broughtId) ?: error("Item not found")
                require(item.event.id.value == eventId) { "Item not found" }
                val isOwner  = item.event.owner.id.value == userId
                val isAuthor = item.user.id.value == userId
                require(isOwner || isAuthor) { "Access denied" }
                item.delete()
            }
        }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun ItemRequestEntity.toResponse() = ItemRequestResponse(
        id             = id.value,
        label          = label,
        quantity       = quantity,
        isFulfilled    = isFulfilled,
        assignedToName = assignedTo?.displayName,
        categoryId     = category?.id?.value,
        categoryLabel  = category?.label,
        categoryIcon   = category?.icon,
    )

    private fun ItemBroughtEntity.toResponse() = ItemBroughtResponse(
        id            = id.value,
        label         = label,
        quantity      = quantity,
        userId        = user.id.value,
        userName      = user.displayName,
        categoryId    = category?.id?.value,
        categoryLabel = category?.label,
        categoryIcon  = category?.icon,
    )
}
