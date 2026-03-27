package com.partyplanner.domain.model

data class ItemRequest(
    val id: Int,
    val label: String,
    val quantity: Int,
    val isFulfilled: Boolean,
    val assignedToName: String?,
    val categoryId: Int?,
    val categoryLabel: String?,
    val categoryIcon: String?,
)

data class ItemBrought(
    val id: Int,
    val label: String,
    val quantity: Int,
    val userId: Int,
    val userName: String,
    val categoryId: Int?,
    val categoryLabel: String?,
    val categoryIcon: String?,
)

data class EventItems(
    val requests: List<ItemRequest>,
    val brought: List<ItemBrought>,
)
