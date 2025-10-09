package com.evcharging.evchargingapp.data

enum class UserRole {
    EV_OWNER,
    STATION_OPERATOR
}

data class User(
    val nic: String,
    val name: String,
    val contactNumber: String,
    val passwordHash: String,
    val role: UserRole,
    var isActive: Boolean = true
)
