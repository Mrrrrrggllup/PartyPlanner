package com.partyplanner.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: Int,
    val label: String,
    val icon: String?,
)

@Serializable
data class AddItemRequestDto(
    val label: String,
    val quantity: Int = 1,
    val categoryId: Int? = null,
)

@Serializable
data class AddItemBroughtDto(
    val label: String,
    val quantity: Int = 1,
    val categoryId: Int? = null,
)

@Serializable
data class ItemRequestResponse(
    val id: Int,
    val label: String,
    val quantity: Int,
    val isFulfilled: Boolean,
    val assignedToName: String?,
    val categoryId: Int?,
    val categoryLabel: String?,
    val categoryIcon: String?,
)

@Serializable
data class ItemBroughtResponse(
    val id: Int,
    val label: String,
    val quantity: Int,
    val userId: Int,
    val userName: String,
    val categoryId: Int?,
    val categoryLabel: String?,
    val categoryIcon: String?,
)

@Serializable
data class ItemsResponse(
    val requests: List<ItemRequestResponse>,
    val brought: List<ItemBroughtResponse>,
)
