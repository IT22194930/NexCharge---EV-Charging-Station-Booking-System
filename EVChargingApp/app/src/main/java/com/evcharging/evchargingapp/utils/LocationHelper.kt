package com.evcharging.evchargingapp.utils

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object LocationHelper {
    
    /**
     * Check if GPS is enabled on the device
     */
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    /**
     * Check if network location is enabled
     */
    fun isNetworkLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Check if any location provider is enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        return isGpsEnabled(context) || isNetworkLocationEnabled(context)
    }
    
    /**
     * Show dialog to enable location services
     */
    fun showLocationSettingsDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Location Services Required")
            .setMessage("To show your location on the map, please enable location services in your device settings.")
            .setIcon(android.R.drawable.ic_dialog_map)
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("LocationHelper", "Failed to open location settings", e)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}