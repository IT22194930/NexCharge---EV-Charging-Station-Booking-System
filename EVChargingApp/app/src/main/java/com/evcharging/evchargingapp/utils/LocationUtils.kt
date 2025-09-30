package com.evcharging.evchargingapp.utils

import com.google.android.gms.maps.model.LatLng

object LocationUtils {
    
    /**
     * Parse location string to LatLng
     * Supports various formats:
     * - "lat,lng" (e.g., "6.9271,79.8612")
     * - "lat, lng" (with space)
     * - Default Sri Lanka locations for known cities
     */
    fun parseLocation(locationString: String?): LatLng? {
        if (locationString.isNullOrBlank()) return null
        
        return try {
            // Try to parse as coordinates first
            val parts = locationString.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                
                if (lat != null && lng != null) {
                    return LatLng(lat, lng)
                }
            }
            
            // Fallback to known locations if parsing fails
            getDefaultLocationForCity(locationString.trim())
            
        } catch (e: Exception) {
            // Return default Sri Lanka location
            LatLng(7.8731, 80.7718)
        }
    }
    
    /**
     * Get default coordinates for major Sri Lankan cities
     */
    private fun getDefaultLocationForCity(cityName: String): LatLng {
        return when (cityName.lowercase()) {
            "colombo" -> LatLng(6.9271, 79.8612)
            "kandy" -> LatLng(7.2906, 80.6337)
            "galle" -> LatLng(6.0535, 80.2210)
            "jaffna" -> LatLng(9.6615, 80.0255)
            "negombo" -> LatLng(7.2083, 79.8358)
            "anuradhapura" -> LatLng(8.3114, 80.4037)
            "trincomalee" -> LatLng(8.5874, 81.2152)
            "batticaloa" -> LatLng(7.7102, 81.6020)
            "kurunegala" -> LatLng(7.4863, 80.3647)
            "ratnapura" -> LatLng(6.6828, 80.3992)
            "badulla" -> LatLng(6.9934, 81.0550)
            "matara" -> LatLng(5.9549, 80.5550)
            "kalutara" -> LatLng(6.5854, 79.9607)
            "gampaha" -> LatLng(7.0873, 79.9990)
            "nuwara eliya" -> LatLng(6.9497, 80.7891)
            else -> LatLng(7.8731, 80.7718) // Default to center of Sri Lanka
        }
    }
    
    /**
     * Calculate distance between two points in kilometers
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
            distanceKm < 10.0 -> "%.1f km".format(distanceKm)
            else -> "%.0f km".format(distanceKm)
        }
    }
}