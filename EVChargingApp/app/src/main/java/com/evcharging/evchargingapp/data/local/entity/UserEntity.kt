package com.evcharging.evchargingapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val nic: String,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String = "EVOwner",
    val isActive: Boolean = true,
    val createdAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis(), // For offline sync
    val isPendingSync: Boolean = false, // For offline changes
    val localChanges: String? = null // JSON of pending changes
)