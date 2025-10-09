package com.evcharging.evchargingapp.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.evcharging.evchargingapp.data.User // Import the User data class
import com.evcharging.evchargingapp.data.UserRole // Import the UserRole enum

class UserDao(context: Context) {

    private val dbHelper = UserDbHelper(context)

    // Function to add a new user
    fun addUser(user: User): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_NIC, user.nic)
            put(UserDbHelper.COLUMN_NAME, user.name)
            put(UserDbHelper.COLUMN_CONTACT_NUMBER, user.contactNumber)
            put(UserDbHelper.COLUMN_PASSWORD_HASH, user.passwordHash)
            put(UserDbHelper.COLUMN_ROLE, user.role.name)
            put(UserDbHelper.COLUMN_IS_ACTIVE, if (user.isActive) 1 else 0)
        }
        val id = db.insert(UserDbHelper.TABLE_USERS, null, values)
        db.close()
        return id
    }

    // Function to get a user by NIC
    fun getUserByNic(nic: String): User? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            UserDbHelper.TABLE_USERS,
            null,
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(nic),
            null,
            null,
            null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_NAME)
            val contactNumberIndex = cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_CONTACT_NUMBER)
            val passwordHashIndex = cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_PASSWORD_HASH)
            val roleIndex = cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_ROLE)
            val isActiveIndex = cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_IS_ACTIVE)

            val name = cursor.getString(nameIndex)
            val contactNumber = cursor.getString(contactNumberIndex)
            val passwordHash = cursor.getString(passwordHashIndex)
            val roleString = cursor.getString(roleIndex)
            val isActiveInt = cursor.getInt(isActiveIndex)

            // Convert roleString to UserRole enum
            val userRole = try {
                UserRole.valueOf(roleString.uppercase())
            } catch (e: IllegalArgumentException) {
                if (nic.startsWith("EV")) UserRole.EV_OWNER else UserRole.STATION_OPERATOR
            }

            user = User(nic, name, contactNumber, passwordHash, userRole, isActiveInt == 1)
        }
        cursor.close()
        db.close()
        return user
    }

    // Function to update a user's details
    fun updateUser(user: User): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_NAME, user.name)
            put(UserDbHelper.COLUMN_CONTACT_NUMBER, user.contactNumber)
            put(UserDbHelper.COLUMN_PASSWORD_HASH, user.passwordHash)
            put(UserDbHelper.COLUMN_ROLE, user.role.name)
            put(UserDbHelper.COLUMN_IS_ACTIVE, if (user.isActive) 1 else 0)
        }
        val rowsAffected = db.update(
            UserDbHelper.TABLE_USERS,
            values,
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(user.nic)
        )
        db.close()
        return rowsAffected
    }

    // Function to deactivate a user
    fun deactivateUser(nic: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_IS_ACTIVE, 0)
        }
        val rowsAffected = db.update(
            UserDbHelper.TABLE_USERS,
            values,
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(nic)
        )
        db.close()
        return rowsAffected
    }

    // Function to reactivate a user
    fun reactivateUser(nic: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_IS_ACTIVE, 1)
        }
        val rowsAffected = db.update(
            UserDbHelper.TABLE_USERS,
            values,
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(nic)
        )
        db.close()
        return rowsAffected
    }
}
