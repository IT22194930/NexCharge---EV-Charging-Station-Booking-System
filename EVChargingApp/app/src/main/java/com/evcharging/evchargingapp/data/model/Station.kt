package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class Station(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("availableSlots")
    val availableSlots: Int,
    
    @SerializedName("isActive")
    val isActive: Boolean = true,
    
    @SerializedName("operatingHours")
    val operatingHours: Any? = null
)

data class StationCreateRequest(
    val name: String,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String,
    val availableSlots: Int
)

data class StationUpdateRequest(
    val name: String,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String,
    val availableSlots: Int,
    val isActive: Boolean
)