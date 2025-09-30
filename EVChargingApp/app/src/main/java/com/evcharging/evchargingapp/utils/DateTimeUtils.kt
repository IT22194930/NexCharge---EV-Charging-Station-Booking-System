package com.evcharging.evchargingapp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    
    private val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
    )
    
    private val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    
    /**
     * Formats a datetime string to a user-friendly format
     * Handles various input formats and converts to: "Jan 15, 2024 at 08:00 AM"
     */
    fun formatToUserFriendly(dateTimeString: String): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateTimeString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                // Fallback: return original string if parsing fails
                dateTimeString
            }
        } catch (e: Exception) {
            // Fallback: return original string if any error occurs
            dateTimeString
        }
    }
    
    /**
     * Formats a datetime string to a simple date format
     * Returns: "Jan 15, 2024"
     */
    fun formatToSimpleDate(dateTimeString: String): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateTimeString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(parsedDate)
            } else {
                dateTimeString
            }
        } catch (e: Exception) {
            dateTimeString
        }
    }
    
    /**
     * Formats a datetime string to show only time
     * Returns: "08:00 AM"
     */
    fun formatToTime(dateTimeString: String): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateTimeString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(parsedDate)
            } else {
                dateTimeString
            }
        } catch (e: Exception) {
            dateTimeString
        }
    }
    
    /**
     * Formats a datetime string to show relative time (for booking lists)
     * Returns: "Jan 15, 08:00 AM"
     */
    fun formatRelative(dateTimeString: String): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateTimeString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(parsedDate)
            } else {
                dateTimeString
            }
        } catch (e: Exception) {
            dateTimeString
        }
    }
    
    /**
     * Formats a datetime string to show short format (for recent bookings)
     * Returns: "Jan 15, 08:00 AM"
     */
    fun formatShort(dateTimeString: String): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateTimeString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(parsedDate)
            } else {
                dateTimeString
            }
        } catch (e: Exception) {
            dateTimeString
        }
    }
}