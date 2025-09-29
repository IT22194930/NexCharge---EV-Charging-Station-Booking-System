package com.evcharging.evchargingapp.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    
    private const val TAG = "DateTimeUtils"
    
    // Common date formats from API
    private val inputFormats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "dd/MM/yyyy HH:mm",
        "MM/dd/yyyy HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    )
    
    // Output formats for user display
    private val userFriendlyFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    /**
     * Convert any date/time string to user-friendly format
     * Examples:
     * - "2024-12-25 14:30:00" -> "Dec 25, 2024 at 2:30 PM"
     * - "25/12/2024 14:30" -> "Dec 25, 2024 at 2:30 PM"
     */
    fun formatToUserFriendly(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "Not scheduled"
        
        try {
            // Try parsing with different formats
            for (formatPattern in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    if (date != null) {
                        return userFriendlyFormat.format(date)
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            // If no format worked, try to extract meaningful parts
            return extractAndFormat(dateTimeString)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateTimeString", e)
            return dateTimeString // Return original if all else fails
        }
    }
    
    /**
     * Format date only (no time)
     * Example: "Dec 25, 2024"
     */
    fun formatDateOnly(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "No date"
        
        try {
            for (formatPattern in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    if (date != null) {
                        return dateOnlyFormat.format(date)
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            // Fallback: try to extract date part
            val datePart = dateTimeString.split(" ").firstOrNull() ?: dateTimeString
            return formatToUserFriendly(datePart).split(" at ").firstOrNull() ?: datePart
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date only: $dateTimeString", e)
            return dateTimeString
        }
    }
    
    /**
     * Format time only (no date)
     * Example: "2:30 PM"
     */
    fun formatTimeOnly(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "No time"
        
        try {
            for (formatPattern in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    if (date != null) {
                        return timeOnlyFormat.format(date)
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            // Fallback: try to extract time part
            val parts = dateTimeString.split(" ")
            if (parts.size >= 2) {
                val timePart = parts[1]
                try {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val time = timeFormat.parse(timePart)
                    if (time != null) {
                        return timeOnlyFormat.format(time)
                    }
                } catch (e: Exception) {
                    // Return original time part
                    return timePart
                }
            }
            
            return dateTimeString
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time only: $dateTimeString", e)
            return dateTimeString
        }
    }
    
    /**
     * Format for short display (useful in cards)
     * Example: "Dec 25" or "Dec 25, 2:30 PM"
     */
    fun formatShort(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "No date"
        
        try {
            for (formatPattern in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        val today = Calendar.getInstance()
                        calendar.time = date
                        
                        return when {
                            isSameDay(calendar, today) -> "Today, ${timeOnlyFormat.format(date)}"
                            isTomorrow(calendar, today) -> "Tomorrow, ${timeOnlyFormat.format(date)}"
                            isSameYear(calendar, today) -> "${shortDateFormat.format(date)}, ${timeOnlyFormat.format(date)}"
                            else -> "${dateOnlyFormat.format(date)}, ${timeOnlyFormat.format(date)}"
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            return dateTimeString
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting short: $dateTimeString", e)
            return dateTimeString
        }
    }
    
    /**
     * Get relative time description
     * Examples: "Today at 2:30 PM", "Tomorrow at 10:00 AM", "Dec 25 at 3:15 PM"
     */
    fun formatRelative(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "Not scheduled"
        
        try {
            for (formatPattern in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        val today = Calendar.getInstance()
                        calendar.time = date
                        
                        return when {
                            isSameDay(calendar, today) -> "Today at ${timeOnlyFormat.format(date)}"
                            isTomorrow(calendar, today) -> "Tomorrow at ${timeOnlyFormat.format(date)}"
                            isYesterday(calendar, today) -> "Yesterday at ${timeOnlyFormat.format(date)}"
                            else -> "${shortDateFormat.format(date)} at ${timeOnlyFormat.format(date)}"
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            return formatToUserFriendly(dateTimeString)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting relative: $dateTimeString", e)
            return dateTimeString
        }
    }
    
    private fun extractAndFormat(dateTimeString: String): String {
        try {
            // Try to extract date and time components manually
            val parts = dateTimeString.split(" ")
            if (parts.size >= 2) {
                val datePart = parts[0]
                val timePart = parts[1]
                
                // Try different date patterns
                val datePatterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy")
                for (pattern in datePatterns) {
                    try {
                        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                        val date = dateFormat.parse(datePart)
                        if (date != null) {
                            val calendar = Calendar.getInstance()
                            calendar.time = date
                            
                            // Parse time
                            val timePattern = if (timePart.contains(":")) {
                                if (timePart.split(":").size == 3) "HH:mm:ss" else "HH:mm"
                            } else "HH:mm"
                            
                            try {
                                val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
                                val time = timeFormat.parse(timePart)
                                if (time != null) {
                                    val timeCalendar = Calendar.getInstance()
                                    timeCalendar.time = time
                                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                    calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
                                    
                                    return userFriendlyFormat.format(calendar.time)
                                }
                            } catch (e: Exception) {
                                // Return with original time
                                return "${dateOnlyFormat.format(date)} at $timePart"
                            }
                        }
                    } catch (e: Exception) {
                        // Continue to next pattern
                    }
                }
            }
            
            return dateTimeString
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractAndFormat: $dateTimeString", e)
            return dateTimeString
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun isTomorrow(cal1: Calendar, today: Calendar): Boolean {
        val tomorrow = Calendar.getInstance()
        tomorrow.time = today.time
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        return isSameDay(cal1, tomorrow)
    }
    
    private fun isYesterday(cal1: Calendar, today: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.time = today.time
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal1, yesterday)
    }
    
    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
}