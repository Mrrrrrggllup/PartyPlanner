package com.partyplanner.domain.model

data class User(
    val id: Int,
    val email: String,
    val displayName: String,
    val phone: String? = null
)