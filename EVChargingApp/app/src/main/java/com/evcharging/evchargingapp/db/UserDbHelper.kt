package com.evcharging.evchargingapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "UserDatabase.db"

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "_id"
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_CONTACT_NUMBER = "contact_number"
        const val COLUMN_PASSWORD_HASH = "password_hash"
        const val COLUMN_ROLE = "role"
        const val COLUMN_IS_ACTIVE = "is_active"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE_USERS = ("CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NIC + " TEXT UNIQUE,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_CONTACT_NUMBER + " TEXT,"
                + COLUMN_PASSWORD_HASH + " TEXT,"
                + COLUMN_ROLE + " TEXT,"
                + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1" + ")")
        db.execSQL(CREATE_TABLE_USERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) { 
        }
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}
