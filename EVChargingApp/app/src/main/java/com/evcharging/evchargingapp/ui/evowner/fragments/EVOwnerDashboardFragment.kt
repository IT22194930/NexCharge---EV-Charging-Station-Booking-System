package com.evcharging.evchargingapp.ui.evowner.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.util.*
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerDashboardBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.DashboardStats
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.repository.UserRepository
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import com.evcharging.evchargingapp.utils.LocationUtils
import com.evcharging.evchargingapp.utils.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class EVOwnerDashboardFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentEvownerDashboardBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private val userRepository by lazy { UserRepository(requireContext()) }
    private var userName: String = "EV Owner" // Store user's full name
    
    // Google Maps related
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: LatLng? = null
    private var stationsList: List<Station> = emptyList()
    private var locationCallback: LocationCallback? = null
    private var isLocationRequested = false
    
    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        Log.d("EVOwnerDashboard", "Permission results: Fine=$fineLocationGranted, Coarse=$coarseLocationGranted")
        
        when {
            fineLocationGranted -> {
                Log.d("EVOwnerDashboard", "Fine location permission granted")
                Toast.makeText(requireContext(), "Location permission granted! 📍", Toast.LENGTH_SHORT).show()
                enableUserLocation()
            }
            coarseLocationGranted -> {
                Log.d("EVOwnerDashboard", "Coarse location permission granted")
                Toast.makeText(requireContext(), "Approximate location permission granted 📍", Toast.LENGTH_SHORT).show()
                enableUserLocation()
            }
            else -> {
                Log.w("EVOwnerDashboard", "Location permissions denied")
                Toast.makeText(
                    requireContext(), 
                    "Location permission is required to show your location on the map", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        val userRole = TokenUtils.getUserRole(requireContext())
        val tokenValid = TokenUtils.isTokenValid(requireContext())
        
        if (userNic == null) {
            Log.e("EVOwnerDashboardFragment", "User NIC is null - authentication issue")
            Toast.makeText(requireContext(), "Authentication issue: Please login again", Toast.LENGTH_LONG).show()
            return
        }
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupDashboard()
        setupFontSizes()
        setupMapFragment()
        
        LoadingManager.show(requireContext(), "Loading your dashboard...")
        loadUserProfileAndDashboard()
    }

    private fun setupMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupDashboard() {
        // Set initial loading message with professional greeting
        val greeting = getTimeBasedGreeting()
        binding.textViewWelcome.text = "$greeting\nLoading your dashboard..."
        
        // Setup click listeners for quick actions
        binding.buttonAddStation.setOnClickListener {
            // Navigate to My Bookings tab
            if (isAdded && context != null) {
                navigateToBookingsTab()
            }
        }
        
        binding.buttonViewReports.setOnClickListener {
            // Navigate to Reservations tab
            if (isAdded && context != null) {
                navigateToReservationsTab()
            }
        }
        
        // Setup locate me button
        binding.buttonLocateMe.setOnClickListener {
            Log.d("EVOwnerDashboard", "Locate me button clicked")
            
            if (userLocation != null) {
                // If we already have user location, just center the map
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f),
                    1500,
                    null
                )
                Toast.makeText(requireContext(), "Centered on your location 📍", Toast.LENGTH_SHORT).show()
            } else {
                // Try to get location again
                checkLocationPermissionAndGetLocation()
            }
        }
        
        // Setup find stations button
        binding.buttonFindStations.setOnClickListener {
            Log.d("EVOwnerDashboard", "Find stations button clicked")
            
            if (stationsList.isNotEmpty()) {
                // Show all stations on map with optimal zoom
                showAllStationsOnMap()
                Toast.makeText(requireContext(), "Showing ${stationsList.size} charging stations", Toast.LENGTH_SHORT).show()
            } else {
                // Reload stations if list is empty
                Toast.makeText(requireContext(), "Loading charging stations...", Toast.LENGTH_SHORT).show()
                loadChargingStationsForMap()
            }
        }
    }

    private fun setupFontSizes() {
        // Set appropriate font sizes for different text elements
        binding.apply {
            // Welcome message - Large and prominent
            textViewWelcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            
            // Dashboard stats - Only My Bookings and Active Bookings
            textViewTotalStations.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // Will show My Bookings
            textViewActiveBookings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // Will show Active Bookings
            

            
            // Button text - Rename buttons for new functionality
            buttonAddStation.text = "My Bookings"
            buttonAddStation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            buttonViewReports.text = "Book Now"
            buttonViewReports.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
    }

    private fun loadUserProfileAndDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // First load user profile
                LoadingManager.updateMessage("Loading profile...")
                loadUserProfileSync()
                
                // Then load dashboard data
                LoadingManager.updateMessage("Loading dashboard data...")
                loadDashboardDataSync()
                
                // Load stations for map if map is ready
                if (googleMap != null) {
                    LoadingManager.updateMessage("Loading charging stations...")
                    loadChargingStationsForMap()
                }
                
            } catch (e: Exception) {
                Log.e("EVOwnerDashboardFragment", "Error loading data", e)
                if (isAdded && _binding != null) {
                    showError("Error loading data: ${e.message}")
                }
            } finally {
                // Dismiss loading only after all operations complete
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                }
            }
        }
    }

    private suspend fun loadUserProfileSync() {
        try {
            // Use repository which handles local/remote data
            val owner = userRepository.getProfile()
            
            if (!isAdded || _binding == null) return
            
            if (owner != null) {
                userName = when {
                    owner.FullName.isNotBlank() -> owner.FullName
                    owner.actualNic.isNotBlank() -> owner.actualNic
                    else -> "EV Owner"
                }
                
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            } else {
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                userName = userNic ?: "EV Owner"
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboardFragment", "Error loading user profile", e)
            
            if (isAdded && _binding != null) {
                val userNic = try {
                    TokenUtils.getCurrentUserNic(requireContext())
                } catch (contextError: Exception) {
                    Log.w("EVOwnerDashboardFragment", "Context not available for fallback", contextError)
                    null
                }
                userName = userNic ?: "EV Owner"
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            }
        }
    }

    private suspend fun loadDashboardDataSync() {
        try {
            val response = apiService.getDashboardStats()
            
            if (!isAdded || _binding == null) return
            
            if (response.isSuccessful && response.body() != null) {
                val stats = response.body()!!
                updateDashboardUI(stats)
            } else {
                val errorMsg = "Failed to load dashboard data. Response code: ${response.code()}"
                Log.e("EVOwnerDashboardFragment", errorMsg)
                showError(errorMsg)
                updateDashboardUI(DashboardStats())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w("EVOwnerDashboardFragment", "Dashboard data request was cancelled")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("EVOwnerDashboardFragment", "Request timed out", e)
            if (isAdded && _binding != null) {
                showError("Request timed out. Please check your internet connection.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: java.net.ConnectException) {
            Log.e("EVOwnerDashboardFragment", "Connection failed", e)
            if (isAdded && _binding != null) {
                showError("Cannot connect to server. Please check if the API server is running.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("EVOwnerDashboardFragment", "Unknown host", e)
            if (isAdded && _binding != null) {
                showError("Cannot reach server. Please check your network connection.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboardFragment", "Unexpected error loading dashboard data", e)
            if (isAdded && _binding != null) {
                showError("Network error: ${e.message}")
                // Show default values on error
                updateDashboardUI(DashboardStats())
            }
        }
    }

    private fun loadDashboardData() {
        
    }

    private fun loadUserProfile() {
        // This method is kept for backward compatibility but not used in the new flow
        // All loading is now handled by loadUserProfileAndDashboard()
    }

    private fun updateDashboardUI(stats: DashboardStats) {
        binding.textViewTotalStations.text = "My Bookings\n${stats.totalBookings}"
        binding.textViewActiveBookings.text = "Active Bookings\n${stats.activeBookings}"
    }

    private fun calculateRevenue(stats: DashboardStats): String {
        val estimatedRevenue = stats.totalBookings * 25.0
        return String.format("%.2f", estimatedRevenue)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isAdded && _binding != null) {
            binding.buttonAddStation.isEnabled = !isLoading
            binding.buttonViewReports.isEnabled = !isLoading
        }
    }

    private fun navigateToBookingsTab() {
        try {
            val activity = requireActivity()
            if (activity is com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity) {
                activity.switchToTab(R.id.nav_bookings)
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error navigating to bookings tab", e)
            Toast.makeText(requireContext(), "Error navigating to bookings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToReservationsTab() {
        // Navigate to Reservations tab in the bottom navigation
        try {
            val activity = requireActivity()
            if (activity is com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity) {
                activity.switchToTab(R.id.nav_reservations)
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error navigating to reservations tab", e)
            Toast.makeText(requireContext(), "Error navigating to reservations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        // Check if fragment is still attached before showing toast
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Welcome Back"
        }
    }

    private fun getCreativeWelcomeMessage(userName: String, greeting: String): String {
        return "$greeting\n$userName"
    }
    
    // Google Maps Implementation
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = false // We have our own button
            uiSettings.isMapToolbarEnabled = true
            
            // Set default location to Sri Lanka
            val sriLanka = LatLng(7.8731, 80.7718)
            moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7f))
        }
        
        // Load charging stations and show on map
        loadChargingStationsForMap()
        
        // Check for location permission and enable location if granted
        checkLocationPermissionAndGetLocation()
    }
    
    private fun checkLocationPermissionAndGetLocation() {
        // First check if location services are enabled
        if (!LocationHelper.isLocationEnabled(requireContext())) {
            Log.w("EVOwnerDashboard", "Location services are disabled")
            LocationHelper.showLocationSettingsDialog(requireContext())
            return
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("EVOwnerDashboard", "Location permission granted, enabling location")
                enableUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d("EVOwnerDashboard", "Showing permission rationale")
                Toast.makeText(
                    requireContext(),
                    "Location permission is needed to show your location on the map",
                    Toast.LENGTH_LONG
                ).show()
                requestLocationPermissions()
            }
            else -> {
                Log.d("EVOwnerDashboard", "Requesting location permissions")
                requestLocationPermissions()
            }
        }
    }
    
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("EVOwnerDashboard", "Location permission not granted")
            return
        }

        try {
            googleMap?.isMyLocationEnabled = true
            
            // Show loading message
            Toast.makeText(requireContext(), "Getting your location... 📍", Toast.LENGTH_SHORT).show()
            
            // First try to get last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("EVOwnerDashboard", "Got last known location: ${location.latitude}, ${location.longitude}")
                    updateUserLocation(location)
                } else {
                    Log.d("EVOwnerDashboard", "No last known location, requesting fresh location")
                    requestFreshLocation()
                }
            }.addOnFailureListener { exception ->
                Log.e("EVOwnerDashboard", "Failed to get last location", exception)
                requestFreshLocation()
            }
            
        } catch (e: SecurityException) {
            Log.e("EVOwnerDashboard", "Security exception when enabling location", e)
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Unexpected error enabling location", e)
            Toast.makeText(requireContext(), "Error accessing location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || isLocationRequested
        ) {
            return
        }
        
        isLocationRequested = true
        
        // Create location request for fresh location
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()
        
        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d("EVOwnerDashboard", "Got fresh location: ${location.latitude}, ${location.longitude}")
                    updateUserLocation(location)
                    stopLocationUpdates()
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                requireActivity().mainLooper
            )
            
            // Stop location updates after 30 seconds if no location is found
            android.os.Handler(requireActivity().mainLooper).postDelayed({
                if (isLocationRequested && userLocation == null) {
                    stopLocationUpdates()
                    Toast.makeText(
                        requireContext(), 
                        "Unable to get precise location. Please check GPS settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Use approximate location based on country
                    useDefaultLocation()
                }
            }, 30000L)
            
        } catch (e: SecurityException) {
            Log.e("EVOwnerDashboard", "Security exception requesting location", e)
            isLocationRequested = false
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error requesting location", e)
            isLocationRequested = false
        }
    }
    
    private fun updateUserLocation(location: Location) {
        userLocation = LatLng(location.latitude, location.longitude)
        
        googleMap?.let { map ->
            // Move camera to user location
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(userLocation!!, 14f),
                2000,
                null
            )
            
            // Clear previous markers and re-add everything
            map.clear()
            
            // Add user location marker
            map.addMarker(
                MarkerOptions()
                    .position(userLocation!!)
                    .title("📍 Your Current Location")
                    .snippet("Accuracy: ${location.accuracy.toInt()}m")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            
            // Refresh station markers to include distance info
            displayStationsOnMap()
        }
        
        Toast.makeText(requireContext(), "Location found! 📍 (±${location.accuracy.toInt()}m)", Toast.LENGTH_SHORT).show()
        
        Log.d("EVOwnerDashboard", "User location updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
        isLocationRequested = false
    }
    
    private fun useDefaultLocation() {
        // Use Sri Lanka as default map view without adding any markers
        val defaultLocation = LatLng(7.8731, 80.7718) // Center of Sri Lanka
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(defaultLocation, 7f)
        )
        
        Toast.makeText(requireContext(), "Showing all charging stations in Sri Lanka", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadChargingStationsForMap() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllStations()
                
                if (response.isSuccessful && response.body() != null) {
                    stationsList = response.body()!!
                    displayStationsOnMap()
                } else {
                    Log.e("EVOwnerDashboard", "Failed to load stations for map: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerDashboard", "Error loading stations for map", e)
            }
        }
    }
    
    private fun displayStationsOnMap() {
        googleMap?.let { map ->
            for (station in stationsList) {
                try {
                    // Use LocationUtils to parse location, or use lat/lng if available
                    val stationLocation = if (station.latitude != null && station.longitude != null) {
                        LatLng(station.latitude, station.longitude)
                    } else {
                        LocationUtils.parseLocation(station.location)
                    }
                    
                    if (stationLocation != null) {
                        // Determine marker color based on station active status
                        val markerColor = if (station.isActive) {
                            BitmapDescriptorFactory.HUE_GREEN
                        } else {
                            BitmapDescriptorFactory.HUE_RED
                        }
                        
                        // Calculate distance from user location if available
                        val distanceText = userLocation?.let { userLoc ->
                            val distance = LocationUtils.calculateDistance(userLoc, stationLocation)
                            " • ${LocationUtils.formatDistance(distance)} away"
                        } ?: ""
                        
                        val statusText = if (station.isActive) "Active" else "Inactive"
                        
                        map.addMarker(
                            MarkerOptions()
                                .position(stationLocation)
                                .title("⚡ ${station.name}")
                                .snippet("Status: $statusText\nType: ${station.type}\nSlots: ${station.availableSlots}$distanceText")
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                        )
                    } else {
                        Log.w("EVOwnerDashboard", "Could not parse location for station: ${station.name} (${station.location})")
                    }
                } catch (e: Exception) {
                    Log.e("EVOwnerDashboard", "Error parsing station location: ${station.name}", e)
                }
            }
            
            // Show info message if no stations found
            if (stationsList.isEmpty()) {
                Toast.makeText(requireContext(), "No charging stations found", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("EVOwnerDashboard", "Displayed ${stationsList.size} charging stations on map")
            }
        }
    }
    
    private fun showAllStationsOnMap() {
        googleMap?.let { map ->
            if (stationsList.isNotEmpty()) {
                // Clear existing markers and redraw
                map.clear()
                displayStationsOnMap()
                
                // Calculate bounds to show all stations
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                var hasValidLocations = false
                
                for (station in stationsList) {
                    val stationLocation = if (station.latitude != null && station.longitude != null) {
                        LatLng(station.latitude, station.longitude)
                    } else {
                        LocationUtils.parseLocation(station.location)
                    }
                    
                    stationLocation?.let {
                        boundsBuilder.include(it)
                        hasValidLocations = true
                    }
                }
                
                if (hasValidLocations) {
                    try {
                        val bounds = boundsBuilder.build()
                        val padding = 100 // Padding around the bounds
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, padding),
                            2000,
                            null
                        )
                    } catch (e: Exception) {
                        Log.e("EVOwnerDashboard", "Error setting camera bounds", e)
                        // Fallback to default Sri Lanka view
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(7.8731, 80.7718), 7f)
                        )
                    }
                } else {
                    // No valid locations, show default Sri Lanka view
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(7.8731, 80.7718), 7f)
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        // Stop location updates to prevent memory leaks
        stopLocationUpdates()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerDashboardFragment()
    }
}