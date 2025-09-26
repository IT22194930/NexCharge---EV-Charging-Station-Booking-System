package com.evcharging.evchargingapp.data.model

data class Station(
    val id: String,
    val name: String,
    val location: String,
    val ownerId: String,
    val status: String, // Active, Inactive, Maintenance
    val chargerType: String,
    val pricePerHour: Double,
    val isAvailable: Boolean,
    val createdAt: String
)

data class StationCreateRequest(
    val name: String,
    val location: String,
    val chargerType: String,
    val pricePerHour: Double
)

data class StationUpdateRequest(
    val name: String,
    val location: String,
    val chargerType: String,
    val pricePerHour: Double,
    val status: String
)