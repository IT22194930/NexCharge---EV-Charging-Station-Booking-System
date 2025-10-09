package com.evcharging.evchargingapp.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.evcharging.evchargingapp.data.local.database.AppDatabase
import com.evcharging.evchargingapp.data.local.entity.UserEntity
import com.evcharging.evchargingapp.data.model.EVOwner
import com.evcharging.evchargingapp.data.model.EVOwnerCreateRequest
import com.evcharging.evchargingapp.data.model.EVOwnerUpdateRequest
import com.evcharging.evchargingapp.data.model.EVOwnerChangePasswordRequest
import com.evcharging.evchargingapp.data.model.MessageResponse
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.utils.TokenUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response

class UserRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val apiService = RetrofitInstance.createApiService(context)
    private val gson = Gson()
    
    companion object {
        private const val TAG = "UserRepository"
    }
    
    // Local database operations
    suspend fun getUserFromLocal(nic: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByNic(nic)
        }
    }
    
    fun getUserFromLocalFlow(nic: String): Flow<UserEntity?> {
        return userDao.getUserByNicFlow(nic)
    }
    
    suspend fun saveUserToLocal(user: EVOwner) {
        withContext(Dispatchers.IO) {
            val userEntity = UserEntity(
                nic = user.actualNic,
                fullName = user.FullName,
                email = user.email,
                phone = user.phone,
                role = user.Role ?: "EVOwner",
                isActive = user.actualIsActive,
                createdAt = user.actualCreatedAt,
                lastSyncedAt = System.currentTimeMillis(),
                isPendingSync = false
            )
            userDao.insertUser(userEntity)
        }
    }
    
    // Remote API operations
    suspend fun registerEVOwner(request: EVOwnerCreateRequest): Response<EVOwner> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerEVOwner(request)
                if (response.isSuccessful && response.body() != null) {
                    // Save to local database
                    saveUserToLocal(response.body()!!)
                }
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error registering EV Owner", e)
                throw e
            }
        }
    }

    suspend fun getOwnProfile(): Response<EVOwner> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOwnProfile()
                if (response.isSuccessful && response.body() != null) {
                    // Update local database
                    saveUserToLocal(response.body()!!)
                }
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profile", e)
                throw e
            }
        }
    }
    
    suspend fun updateOwnProfile(request: EVOwnerUpdateRequest): Response<*> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateOwnProfile(request)
                if (response.isSuccessful) {
                    // Update local database
                    val currentUser = TokenUtils.getCurrentUserNic(context)
                    currentUser?.let { nic ->
                        val existingUser = userDao.getUserByNic(nic)
                        existingUser?.let { user ->
                            val updatedUser = user.copy(
                                fullName = request.FullName,
                                lastSyncedAt = System.currentTimeMillis(),
                                isPendingSync = false
                            )
                            userDao.updateUser(updatedUser)
                        }
                    }
                }
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                // Mark for sync later if offline
                markProfileUpdateForSync(request)
                throw e
            }
        }
    }
    
    suspend fun changePassword(request: EVOwnerChangePasswordRequest): Response<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.changePassword(request)
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error changing password", e)
                throw e
            }
        }
    }
    
    suspend fun deactivateOwnAccount(): Response<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deactivateOwnAccount()
                if (response.isSuccessful) {
                    // Update local database
                    val currentUser = TokenUtils.getCurrentUserNic(context)
                    currentUser?.let { nic ->
                        userDao.updateUserActiveStatus(nic, false)
                    }
                }
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error deactivating account", e)
                throw e
            }
        }
    }
    
    // Offline sync support
    private suspend fun markProfileUpdateForSync(request: EVOwnerUpdateRequest) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = TokenUtils.getCurrentUserNic(context)
                currentUser?.let { nic ->
                    val changes = gson.toJson(request)
                    userDao.markForSync(nic, changes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking for sync", e)
            }
        }
    }
    
    suspend fun syncPendingChanges() {
        withContext(Dispatchers.IO) {
            try {
                val pendingUsers = userDao.getPendingSyncUsers()
                for (user in pendingUsers) {
                    try {
                        // Attempt to sync pending changes
                        user.localChanges?.let { changesJson ->
                            val request = gson.fromJson(changesJson, EVOwnerUpdateRequest::class.java)
                            val response = apiService.updateOwnProfile(request)
                            if (response.isSuccessful) {
                                userDao.markAsSynced(user.nic, System.currentTimeMillis())
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync user ${user.nic}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
            }
        }
    }
    
    // Hybrid operations (try remote first, fallback to local)
    suspend fun getProfile(): EVOwner? {
        return try {
            // Try remote first
            val response = getOwnProfile()
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                Log.w(TAG, "Remote profile fetch failed with code: ${response.code()}")
                // Fallback to local
                getProfileFromLocal()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote fetch failed, using local data", e)
            getProfileFromLocal()
        }
    }
    
    private suspend fun getProfileFromLocal(): EVOwner? {
        return try {
            val currentUser = TokenUtils.getCurrentUserNic(context)
            currentUser?.let { nic ->
                val userEntity = userDao.getUserByNic(nic)
                userEntity?.let {
                    EVOwner(
                        nic = it.nic,
                        FullName = it.fullName,
                        email = it.email,
                        phone = it.phone,
                        Role = it.role,
                        isActive = it.isActive,
                        createdAt = it.createdAt
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local profile", e)
            null
        }
    }
}
