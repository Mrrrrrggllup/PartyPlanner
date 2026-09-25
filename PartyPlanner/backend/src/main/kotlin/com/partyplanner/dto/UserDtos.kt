package com.partyplanner.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(val token: String, val platform: String = "android")
