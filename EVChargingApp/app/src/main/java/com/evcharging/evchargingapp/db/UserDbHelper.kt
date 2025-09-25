package com.evcharging.evchargingapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // DATABASE_VERSION will need to be incremented if schema changes and onUpgrade is implemented for data migration
        // For development, if you are okay with losing data on schema change, uninstalling the app
        // will trigger onCreate again with the new schema.
        // If you increment DATABASE_VERSION, you MUST implement onUpgrade properly.
        private const val DATABASE_VERSION = 2 // Incremented version due to schema change
        private const val DATABASE_NAME = "UserDatabase.db"

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "_id" // Standard primary key column name
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_CONTACT_NUMBER = "contact_number"
        const val COLUMN_PASSWORD_HASH = "password_hash"
        const val COLUMN_ROLE = "role" // Definition of COLUMN_ROLE
        const val COLUMN_IS_ACTIVE = "is_active" // Changed from is_active to follow convention
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE_USERS = ("CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," // Added an explicit primary key
                + COLUMN_NIC + " TEXT UNIQUE,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_CONTACT_NUMBER + " TEXT,"
                + COLUMN_PASSWORD_HASH + " TEXT,"
                + COLUMN_ROLE + " TEXT," // Added COLUMN_ROLE to the table definition
                + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1" + ")") // Assuming is_active defaults to 1 (true)
        db.execSQL(CREATE_TABLE_USERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This is a simple upgrade strategy: drop and recreate.
        // For a production app, you would use ALTER TABLE to preserve data.
        if (oldVersion < 2) { // Check if upgrading from a version before COLUMN_ROLE was added
            // Example of how you might add a column if you didn't want to drop the table
            // db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_ROLE TEXT")
        }
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}
