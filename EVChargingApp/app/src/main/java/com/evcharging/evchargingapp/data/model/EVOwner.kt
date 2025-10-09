package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class DashboardStats(
    val totalUsers: Int = 0,
    val totalStations: Int = 0,
    val totalBookings: Int = 0,
    val activeBookings: Int = 0
)

data class EVOwner(
    @SerializedName("nic")
    val nic: String? = null,
    
    @SerializedName("NIC") 
    val NIC: String? = null,
    
    @SerializedName("fullName")
    val FullName: String,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("role")
    val Role: String? = null,
    
    @SerializedName("isActive")
    val isActive: Boolean = true,
    
    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    // Computed properties for consistent access
    val actualNic: String get() = NIC ?: nic ?: ""
    val actualIsActive: Boolean get() = isActive
    val actualCreatedAt: String get() = createdAt ?: ""
}

data class EVOwnerCreateRequest(
    val nic: String,
    val FullName: String,
    val password: String,
    val email: String? = null,
    val phone: String? = null
)

data class EVOwnerUpdateRequest(
    val FullName: String,
    val Password: String? = null,
    val email: String? = null,
    val phone: String? = null
)

data class EVOwnerChangePasswordRequest(
    val CurrentPassword: String,
    val NewPassword: String
)

data class EVOwnerUpdateResponse(
    val message: String,
    val user: EVOwner
)

data class MessageResponse(
    val message: String
)