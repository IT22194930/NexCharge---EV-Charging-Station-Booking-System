package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("nic") val nic: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("contactNumber") val contactNumber: String,
    @SerializedName("role") val role: String,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("assignedStationId") val assignedStationId: String? = null,
    @SerializedName("assignedStationName") val assignedStationName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)