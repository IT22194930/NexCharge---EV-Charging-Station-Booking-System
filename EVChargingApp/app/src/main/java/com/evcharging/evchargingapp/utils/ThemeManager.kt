package com.evcharging.evchargingapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_preferences"
    private const val KEY_THEME_MODE = "theme_mode"
    
    // Theme modes
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2
    
    fun saveTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply()
    }
    
    fun getSavedTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM) // Default to system theme
    }
    
    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    fun getThemeName(themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> "Light Theme"
            THEME_DARK -> "Dark Theme"
            THEME_SYSTEM -> "System Default"
            else -> "System Default"
        }
    }
    
    fun getNextTheme(currentTheme: Int): Int {
        return when (currentTheme) {
            THEME_LIGHT -> THEME_DARK
            THEME_DARK -> THEME_SYSTEM
            THEME_SYSTEM -> THEME_LIGHT
            else -> THEME_LIGHT
        }
    }
}