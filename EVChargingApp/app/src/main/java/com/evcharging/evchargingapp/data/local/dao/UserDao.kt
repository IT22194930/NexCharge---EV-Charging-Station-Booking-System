package com.evcharging.evchargingapp.data.local.dao

import androidx.room.*
import com.evcharging.evchargingapp.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE nic = :nic")
    suspend fun getUserByNic(nic: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE nic = :nic")
    fun getUserByNicFlow(nic: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
    
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE nic = :nic")
    suspend fun deleteUserByNic(nic: String)
    
    @Query("UPDATE users SET isActive = :isActive WHERE nic = :nic")
    suspend fun updateUserActiveStatus(nic: String, isActive: Boolean)
    
    @Query("SELECT * FROM users WHERE isPendingSync = 1")
    suspend fun getPendingSyncUsers(): List<UserEntity>
    
    @Query("UPDATE users SET isPendingSync = 0, localChanges = null, lastSyncedAt = :timestamp WHERE nic = :nic")
    suspend fun markAsSynced(nic: String, timestamp: Long)
    
    @Query("UPDATE users SET isPendingSync = 1, localChanges = :changes WHERE nic = :nic")
    suspend fun markForSync(nic: String, changes: String)
}