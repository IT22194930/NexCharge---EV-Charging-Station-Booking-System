package com.evcharging.evchargingapp.data

// Define an enum for roles for better type safety
enum class UserRole {
    EV_OWNER,
    STATION_OPERATOR
}

data class User(
    val nic: String,
    val name: String,
    val contactNumber: String,
    val passwordHash: String, // This was passwordHash in your LoginActivity, assuming it's stored hashed
    val role: UserRole,      // Added role field
    var isActive: Boolean = true
)
