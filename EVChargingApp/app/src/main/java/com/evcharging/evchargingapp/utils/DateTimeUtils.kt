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

    /**
     * Formats date and hour to show time range for booking
     * Returns: "Jan 15, 2024 - 14:00 to 15:00"
     */
    fun formatBookingTimeRange(dateTimeString: String, hour: Int): String {
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
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateStr = dateFormat.format(parsedDate)
                
                // Format start and end hours
                val startHour = String.format("%02d:00", hour)
                val endHour = String.format("%02d:00", hour + 1)
                
                "$dateStr - $startHour to $endHour"
            } else {
                "$dateTimeString - Hour $hour"
            }
        } catch (e: Exception) {
            "$dateTimeString - Hour $hour"
        }
    }

    /**
     * Formats date and hour to show compact time range for booking
     * Returns: "Jan 15 - 14:00-15:00"
     */
    fun formatBookingTimeRangeCompact(dateTimeString: String, hour: Int): String {
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
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                val dateStr = dateFormat.format(parsedDate)
                
                // Format start and end hours
                val startHour = String.format("%02d:00", hour)
                val endHour = String.format("%02d:00", hour + 1)
                
                "$dateStr - $startHour-$endHour"
            } else {
                "$dateTimeString - $hour:00-${hour+1}:00"
            }
        } catch (e: Exception) {
            "$dateTimeString - $hour:00-${hour+1}:00"
        }
    }

    /**
     * Formats date and time for booking details display
     * Returns: "Dec 25, 2024 at 2:30 PM"
     */
    fun formatDateTime(dateTimeString: String): String {
        return formatToUserFriendly(dateTimeString)
    }

    /**
     * Formats date and hour for booking details display
     * Combines reservation date with reservation hour
     * Returns: "Dec 25, 2024 at 5:00 AM"
     */
    fun formatDateTimeWithHour(dateString: String, hour: Int): String {
        return try {
            var parsedDate: Date? = null
            
            // Try different input formats to parse the date
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(dateString)
                    break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            if (parsedDate != null) {
                // Create calendar and set the hour
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                
                // Format with the correct hour
                outputFormat.format(calendar.time)
            } else {
                // Fallback: format manually
                val hourFormatted = if (hour == 0) {
                    "12:00 AM"
                } else if (hour < 12) {
                    "${hour}:00 AM"
                } else if (hour == 12) {
                    "12:00 PM"
                } else {
                    "${hour - 12}:00 PM"
                }
                "$dateString at $hourFormatted"
            }
        } catch (e: Exception) {
            // Final fallback
            val hourFormatted = if (hour == 0) {
                "12:00 AM"
            } else if (hour < 12) {
                "${hour}:00 AM"
            } else if (hour == 12) {
                "12:00 PM"
            } else {
                "${hour - 12}:00 PM"
            }
            "$dateString at $hourFormatted"
        }
    }

    /**
     * Extracts just the date part from a datetime string
     * Returns: "2025-10-03" from "2025-10-03T00:00:00" or similar formats
     */
    fun extractDateOnly(dateTimeString: String): String {
        return try {
            // First try to parse and format properly
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
                // Format as simple date string
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
            } else {
                // Fallback: extract date part from string
                when {
                    dateTimeString.contains("T") -> {
                        // "2025-10-03T00:00:00" -> "2025-10-03"
                        dateTimeString.substringBefore("T")
                    }
                    dateTimeString.contains(" ") -> {
                        // "2025-10-03 00:00:00" -> "2025-10-03"
                        dateTimeString.substringBefore(" ")
                    }
                    else -> {
                        // Already in date format or unknown format
                        dateTimeString
                    }
                }
            }
        } catch (e: Exception) {
            // Final fallback: try string manipulation
            when {
                dateTimeString.contains("T") -> dateTimeString.substringBefore("T")
                dateTimeString.contains(" ") -> dateTimeString.substringBefore(" ")
                else -> dateTimeString
            }
        }
    }
}