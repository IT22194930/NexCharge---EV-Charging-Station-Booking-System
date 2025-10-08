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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.TextView
import java.util.*
import java.util.Date
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerDashboardBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.DashboardStats
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.BookingCreateRequest
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
        
        when {
            fineLocationGranted -> {
                Toast.makeText(requireContext(), "Location permission granted! ", Toast.LENGTH_SHORT).show()
                enableUserLocation()
            }
            coarseLocationGranted -> {
                Toast.makeText(requireContext(), "Approximate location permission granted ", Toast.LENGTH_SHORT).show()
                enableUserLocation()
            }
            else -> {
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
                Toast.makeText(requireContext(), "Centered on your location ", Toast.LENGTH_SHORT).show()
            } else {
                // Try to get location again
                checkLocationPermissionAndGetLocation()
            }
        }
        
        // Setup find stations button
        binding.buttonFindStations.setOnClickListener {
            if (userLocation != null && stationsList.isNotEmpty()) {
                // Show nearest 3 stations dialog
                showNearestStationsDialog()
            } else if (stationsList.isEmpty()) {
                // Load stations first
                Toast.makeText(requireContext(), "Loading charging stations...", Toast.LENGTH_SHORT).show()
                loadChargingStationsForMap()
            } else {
                // No user location available
                Toast.makeText(requireContext(), "Please enable location to find nearest stations", Toast.LENGTH_SHORT).show()
                checkLocationPermissionAndGetLocation()
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
            Log.d("DashboardAPI", "Starting to load dashboard data...")
            
            // Get current user NIC for filtering bookings
            val userNic = TokenUtils.getCurrentUserNic(requireContext())
            if (userNic == null) {
                Log.e("DashboardAPI", "User NIC is null, cannot load bookings")
                showError("Authentication error: Please login again")
                updateDashboardUI(DashboardStats())
                return
            }
            
            Log.d("DashboardAPI", "Loading bookings for user: $userNic")
            
            // Load bookings for current user only
            val bookingsResponse = apiService.getBookingsByOwner(userNic)
            
            Log.d("DashboardAPI", "API response received: success=${bookingsResponse.isSuccessful}, code=${bookingsResponse.code()}")
            
            if (!isAdded || _binding == null) {
                Log.w("DashboardAPI", "Fragment not attached, skipping UI update")
                return
            }
            
            if (bookingsResponse.isSuccessful && bookingsResponse.body() != null) {
                val allBookings = bookingsResponse.body()!!
                
                Log.d("DashboardStats", "Loaded ${allBookings.size} total bookings for user $userNic")
                
                // If no bookings, show zeros
                if (allBookings.isEmpty()) {
                    Log.d("DashboardStats", "No bookings found for user")
                    updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
                    return
                }
                
                // Calculate approved future reservations count
                val currentDate = java.util.Date()
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                
                Log.d("DashboardStats", "Current date: $currentDate, current hour: $currentHour")
                
                val approvedFutureCount = allBookings.count { booking ->
                    val isApproved = booking.status.equals("Approved", ignoreCase = true)
                    val isFuture = isBookingInFuture(booking, currentDate, currentHour)
                    Log.d("DashboardStats", "Booking ${booking.id}: status='${booking.status}', isApproved=$isApproved, isFuture=$isFuture, date=${booking.reservationDate}, hour=${booking.reservationHour}")
                    isApproved && isFuture
                }
                
                // Calculate pending reservations count
                val pendingCount = allBookings.count { booking ->
                    val isPending = booking.status.equals("Pending", ignoreCase = true)
                    Log.d("DashboardStats", "Booking ${booking.id}: status='${booking.status}', isPending=$isPending")
                    isPending
                }
                
                Log.d("DashboardStats", "Final counts - Approved future reservations: $approvedFutureCount, Pending reservations: $pendingCount")
                
                // Create custom stats with our calculated values
                val customStats = DashboardStats(
                    totalUsers = 0, // Not relevant for EV owner
                    totalStations = 0, // Not relevant for EV owner
                    totalBookings = approvedFutureCount, // Now shows approved future reservations
                    activeBookings = pendingCount // Now shows pending reservations
                )
                
                updateDashboardUI(customStats)
            } else {
                val errorMsg = "Failed to load dashboard data. Response code: ${bookingsResponse.code()}"
                Log.e("EVOwnerDashboardFragment", errorMsg)
                
                if (bookingsResponse.body() == null) {
                    Log.e("EVOwnerDashboardFragment", "Response body is null")
                } else {
                    Log.e("EVOwnerDashboardFragment", "Response not successful")
                }
                
                showError("Could not load booking data")
                // Show zero counts as fallback
                updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w("EVOwnerDashboardFragment", "Dashboard data request was cancelled")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("EVOwnerDashboardFragment", "Request timed out", e)
            if (isAdded && _binding != null) {
                showError("Request timed out. Please check your internet connection.")
                updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
            }
        } catch (e: java.net.ConnectException) {
            Log.e("EVOwnerDashboardFragment", "Connection failed", e)
            if (isAdded && _binding != null) {
                showError("Cannot connect to server. Please check if the API server is running.")
                updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("EVOwnerDashboardFragment", "Unknown host", e)
            if (isAdded && _binding != null) {
                showError("Cannot reach server. Please check your network connection.")
                updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboardFragment", "Unexpected error loading dashboard data", e)
            if (isAdded && _binding != null) {
                showError("Network error: ${e.message}")
                // Show default values on error
                updateDashboardUI(DashboardStats(totalBookings = 0, activeBookings = 0))
            }
        }
    }

    private fun isBookingInFuture(booking: Booking, currentDate: java.util.Date, currentHour: Int): Boolean {
        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val bookingDate = dateFormat.parse(booking.reservationDate)
            
            if (bookingDate == null) return false
            
            // Compare dates
            val calendar = java.util.Calendar.getInstance()
            calendar.time = currentDate
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val todayMidnight = calendar.time
            
            val bookingCalendar = java.util.Calendar.getInstance()
            bookingCalendar.time = bookingDate
            bookingCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            bookingCalendar.set(java.util.Calendar.MINUTE, 0)
            bookingCalendar.set(java.util.Calendar.SECOND, 0)
            bookingCalendar.set(java.util.Calendar.MILLISECOND, 0)
            val bookingMidnight = bookingCalendar.time
            
            when {
                bookingMidnight.after(todayMidnight) -> true // Future date
                bookingMidnight.equals(todayMidnight) -> booking.reservationHour > currentHour // Today but future hour
                else -> false // Past date
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error parsing booking date: ${booking.reservationDate}", e)
            false
        }
    }

    private fun loadDashboardData() {
        
    }

    private fun loadUserProfile() {
        // This method is kept for backward compatibility but not used in the new flow
        // All loading is now handled by loadUserProfileAndDashboard()
    }

    private fun updateDashboardUI(stats: DashboardStats) {
        Log.d("DashboardUI", "Updating UI with stats: totalBookings=${stats.totalBookings}, activeBookings=${stats.activeBookings}")
        binding.textViewTotalStations.text = "Approved\nReservations\n ${stats.totalBookings}"
        binding.textViewActiveBookings.text = "Pending\nReservations\n ${stats.activeBookings}"
        Log.d("DashboardUI", "UI updated successfully")
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
            Toast.makeText(requireContext(), "Getting your location... ", Toast.LENGTH_SHORT).show()
            
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
                    .title(" Your Current Location")
                    .snippet("Accuracy: ${location.accuracy.toInt()}m")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            
            // Refresh station markers to include distance info
            displayStationsOnMap()
        }
        
        Toast.makeText(requireContext(), "Location found!  (±${location.accuracy.toInt()}m)", Toast.LENGTH_SHORT).show()
        
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
    
    private fun showNearestStationsDialog() {
        Log.d("NearestStations", "showNearestStationsDialog called")
        Log.d("NearestStations", "stationsList size: ${stationsList.size}")
        Log.d("NearestStations", "userLocation: $userLocation")
        
        // Debug: Check isActive values in all stations
        stationsList.forEachIndexed { index, station ->
            Log.d("NearestStations", "Station $index: ${station.name}, isActive: ${station.isActive}")
        }
        
        userLocation?.let { userLoc ->
            // Calculate distances and get nearest 3 stations
            val stationsWithDistance = stationsList.mapNotNull { station ->
                Log.d("NearestStations", "Processing station: ${station.name}, lat: ${station.latitude}, lng: ${station.longitude}, location: ${station.location}")
                
                val stationLocation = if (station.latitude != null && station.longitude != null) {
                    LatLng(station.latitude, station.longitude)
                } else {
                    LocationUtils.parseLocation(station.location)
                }
                
                stationLocation?.let {
                    val distance = LocationUtils.calculateDistance(userLoc, it)
                    Log.d("NearestStations", "Station ${station.name} distance: $distance km")
                    Pair(station, distance)
                }
            }.sortedBy { it.second }.take(3)
            
            Log.d("NearestStations", "Found ${stationsWithDistance.size} stations with distance")
            
            if (stationsWithDistance.isNotEmpty()) {
                showProfessionalStationsDialog(stationsWithDistance)
            } else {
                Log.d("NearestStations", "No stations found with valid locations")
                Toast.makeText(requireContext(), "No charging stations found nearby. Please check if stations are available in your area.", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Please enable location to find nearest stations", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showProfessionalStationsDialog(stationsWithDistance: List<Pair<Station, Double>>) {
        Log.d("DialogCreation", "Creating dialog for ${stationsWithDistance.size} stations")
        
        // Create the professional dialog using custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nearest_stations, null)
        
        val stationsContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutStationsContainer)
        val closeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonClose)
        
        Log.d("DialogCreation", "Dialog views found - Container: ${stationsContainer != null}, Button: ${closeButton != null}")
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Add station cards to container
        stationsWithDistance.forEachIndexed { index, (station, distance) ->
            Log.d("DialogCreation", "Creating card $index for station: ${station.name}, isActive: ${station.isActive}")
            
            // Use the new professional layout instead of programmatic creation
            val stationCard = createStationCardFromLayout(station, distance, index + 1)
            stationCard.setOnClickListener {
                dialog.dismiss()
                showStationOnMapAndBooking(station)
            }
            stationsContainer.addView(stationCard)
            Log.d("DialogCreation", "Added station card to container")
            
            // Add divider except for last item
            if (index < stationsWithDistance.size - 1) {
                val divider = createDivider()
                stationsContainer.addView(divider)
            }
        }
        
        // Setup close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
    
    private fun createStationCardFromLayout(station: Station, distance: Double, position: Int): View {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_station_card_nearest, null)
        
        try {
            // Debug logging
            Log.d("StationCard", "Station: ${station.name}, isActive: ${station.isActive}")
            
            // Find views with null safety
            val stationName = cardView.findViewById<TextView>(R.id.textViewStationName)
            val stationLocation = cardView.findViewById<TextView>(R.id.textViewStationLocation)
            val stationType = cardView.findViewById<TextView>(R.id.textViewStationType)
            val stationSlots = cardView.findViewById<TextView>(R.id.textViewStationSlots)
            val distanceText = cardView.findViewById<TextView>(R.id.textViewDistance)
            val statusText = cardView.findViewById<TextView>(R.id.textViewStatus)
            
            // Debug logging for views
            Log.d("StationCard", "statusText found: ${statusText != null}")
            
            // Set data with null checks
            stationName?.text = "#$position ${station.name}"
            stationLocation?.text = station.location
            stationType?.text = "Type: ${station.type}"
            stationSlots?.text = "• ${station.availableSlots} slots"
            distanceText?.text = LocationUtils.formatDistance(distance) + " away"
            
            // Set status with appropriate color
            statusText?.let { status ->
                Log.d("StationCard", "Setting status for ${station.name}: isActive = ${station.isActive}")
                
                // Ensure visibility and proper styling
                status.visibility = View.VISIBLE
                status.textSize = 12f
                status.setPadding(16, 8, 16, 8)
                
                if (station.isActive) {
                    status.text = "ACTIVE"
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_active_background)
                    Log.d("StationCard", "Set status to ACTIVE, text: '${status.text}'")
                } else {
                    status.text = "INACTIVE"
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_cancelled_background)
                    Log.d("StationCard", "Set status to INACTIVE, text: '${status.text}'")
                }
                
                // Force layout update
                status.requestLayout()
                status.invalidate()
                
                Log.d("StationCard", "Final status text: '${status.text}', visibility: ${status.visibility}")
            } ?: Log.e("StationCard", "statusText is null!")
            
        } catch (e: Exception) {
            Log.e("StationCard", "Error setting up station card", e)
        }
        
        return cardView
    }
    
    // Keep the old method for backward compatibility but mark it as deprecated
    @Deprecated("Use createStationCardFromLayout instead")
    private fun createProfessionalCardV2(station: Station, distance: Double, position: Int): View {
        val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
            radius = 16f
            cardElevation = 8f
            // Use theme-aware background color
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            setCardBackgroundColor(typedValue.data)
            isClickable = true
            isFocusable = true
        }
        
        val textView = android.widget.TextView(requireContext()).apply {
            text = buildString {
               
                append("${station.name}\n\n")
                append("Location: ${station.location}\n")
                append("Type: ${station.type} • ${station.availableSlots} slots available\n")
                append("Distance: ${LocationUtils.formatDistance(distance)}\n")
                append("Status: ${if (station.isActive) "ACTIVE" else "INACTIVE"}")
                



            }
            textSize = 16f
            // Use theme-aware text color
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            setTextColor(typedValue.data)
            setPadding(20, 20, 20, 20)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        cardView.addView(textView)
        return cardView
    }
    
    private fun createDivider(): View {
        return android.view.View(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(24, 16, 24, 16)
            }
            // Use theme-aware divider color
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
            setBackgroundColor(typedValue.data)
            alpha = 0.2f
        }
    }

    private fun showStationOnMapAndBooking(station: Station) {
        // Clear existing markers and show only the selected station
        googleMap?.clear()
        
        // Get station location
        val stationLocation = if (station.latitude != null && station.longitude != null) {
            LatLng(station.latitude, station.longitude)
        } else {
            LocationUtils.parseLocation(station.location)
        }
        
        stationLocation?.let { location ->
            // Add marker for selected station
            val markerColor = if (station.isActive) {
                BitmapDescriptorFactory.HUE_GREEN
            } else {
                BitmapDescriptorFactory.HUE_RED
            }
            
            val distanceText = userLocation?.let { userLoc ->
                val distance = LocationUtils.calculateDistance(userLoc, location)
                " • ${LocationUtils.formatDistance(distance)} away"
            } ?: ""
            
            val statusText = if (station.isActive) "Active" else "Inactive"
            
            googleMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("⚡ ${station.name}")
                    .snippet("Status: $statusText\nType: ${station.type}\nSlots: ${station.availableSlots}$distanceText")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
            
            // Show user location if available
            userLocation?.let { userLoc ->
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(userLoc)
                        .title(" Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            }
            
            // Animate camera to show both user and station location
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(location, 15f),
                2000,
                null
            )
            
            // Show booking option dialog
            showBookingOptionsDialog(station)
            
        } ?: run {
            Toast.makeText(requireContext(), "Could not locate station on map", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showBookingOptionsDialog(station: Station) {
        val message = if (station.isActive) {
            " Station located on map!\n\n⚡ ${station.name}\n📍 ${station.location}\n🔌 ${station.availableSlots} slots available\n\nWould you like to book this station?"
        } else {
            " Station located on map!\n\n⚡ ${station.name}\n📍 ${station.location}\n🔴 This station is currently inactive\n\nYou can still view it on the map, but booking is not available."
        }
        
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
                // Show all stations again
                displayStationsOnMap()
            }
        
        if (station.isActive) {
            builder.setPositiveButton("📝 Book Now") { dialog, _ ->
                dialog.dismiss()
                showBookingDialog(station)
            }
        }
        
        builder.show()
    }
    
    private fun showBookingDialog(station: Station) {
        // Create the booking dialog using the same layout as reservations
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_booking, null)
        
        val textViewUserInfo = dialogView.findViewById<TextView>(R.id.textViewUserInfo)
        val autoCompleteStation = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.autoCompleteStation)
        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDate)
        val timeEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextTime)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonCreate = dialogView.findViewById<MaterialButton>(R.id.buttonCreate)
        
        // Set user info
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        textViewUserInfo.text = "NIC: ${userNic ?: "Unknown"}"
        
        // Pre-select the station and make it non-editable
        val stationDisplayName = "⚡ ${station.name} - ${station.location}\n   ${station.type} | ${station.availableSlots} charging slots per hour"
        autoCompleteStation.setText(stationDisplayName)
        autoCompleteStation.isEnabled = false // Disable editing since station is pre-selected
        
        var selectedDate: String? = null
        var selectedHour: Int? = null
        
        // Setup date picker with 7-day limit
        dateEditText.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                dateEditText.setText(date)
                // Clear time selection when date changes
                selectedHour = null
                timeEditText.setText("")
               
            }
        }
        
    
        
        // Setup hour picker (replaces time picker)
        timeEditText.setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            showAvailableHoursPicker(station.id, selectedDate!!) { hour ->
                selectedHour = hour
                timeEditText.setText("${hour}:00 - ${hour + 1}:00")
            }
        }
        
       
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Handle button clicks
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        buttonCreate.setOnClickListener {
            if (selectedDate.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedHour == null) {
                Toast.makeText(requireContext(), "Please select an available hour slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            
            // Create the booking with selected date and hour
            createBookingWithHour(station, selectedDate!!, selectedHour!!)
        }
        
        dialog.show()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Show informative message about date restriction
        Toast.makeText(requireContext(), "You can book up to 7 days in advance", Toast.LENGTH_SHORT).show()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        // Set maximum date to 7 days from now
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.DAY_OF_MONTH, 7)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour, minute, true // 24-hour format
        )
        
        timePickerDialog.show()
    }
    
    private fun isValidReservationDateTime(date: String, time: String): Boolean {
        return try {
            val selectedDateTime = "$date $time"
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val selectedDate = dateFormat.parse(selectedDateTime)
            val currentDate = Date()
            
            // Check if the selected date/time is in the future
            selectedDate != null && selectedDate.after(currentDate)
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error validating date/time: $date $time", e)
            false
        }
    }
    
    private fun createBookingWithDateTime(station: Station, date: String, time: String) {
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        if (userNic == null) {
            Toast.makeText(requireContext(), "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Creating booking...")
                
                // Combine date and time into the required format
                val dateTime = "${date}T${time}:00"
                
                Log.d("EVOwnerDashboard", "Creating booking for station: ${station.id}, dateTime: $dateTime")
                
                val request = BookingCreateRequest(
                    ownerNic = userNic,
                    stationId = station.id,
                    reservationDate = dateTime,
                    reservationHour = 0
                )
                
                val response = apiService.createBooking(request)
                
                LoadingManager.dismiss()
                
                if (response.isSuccessful && response.body() != null) {
                    val booking = response.body()!!
                    Log.d("EVOwnerDashboard", "Successfully created booking: ${booking.id}")
                    
                    showBookingSuccessDialog(booking, station.name)
                } else {
                    Log.e("EVOwnerDashboard", "Failed to create booking: ${response.code()} - ${response.message()}")
                    Toast.makeText(requireContext(), "Failed to create booking. Please try again.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                LoadingManager.dismiss()
                Log.e("EVOwnerDashboard", "Error creating booking", e)
                Toast.makeText(requireContext(), "Error creating booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAvailableHoursPicker(stationId: String, date: String, onHourSelected: (Int) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Loading available hours...")
                
                val response = apiService.getAvailableHours(stationId, date)
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    val availableHours = response.body() ?: emptyList()
                    
                    if (availableHours.isEmpty()) {
                        Toast.makeText(requireContext(), "No available slots for this date. Please select another date.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    // Create hour selection dialog
                    val hourOptions = availableHours.map { hour ->
                        "${hour}:00 - ${hour + 1}:00"
                    }.toTypedArray()
                    
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Available Hour Slots")
                        .setSingleChoiceItems(hourOptions, -1) { dialog, which ->
                            onHourSelected(availableHours[which])
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                        
                } else {
                    Toast.makeText(requireContext(), "Failed to load available hours", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                Log.e("BookingHours", "Error loading available hours", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBookingWithHour(station: Station, date: String, hour: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Creating booking...")
                
                val userNic = TokenUtils.getCurrentUserNic(requireContext()) ?: throw Exception("User not logged in")
                
                val bookingRequest = BookingCreateRequest(
                    ownerNic = userNic,
                    stationId = station.id,
                    reservationDate = date,
                    reservationHour = hour
                )
                
                val response = apiService.createBooking(bookingRequest)
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    val booking = response.body()
                    if (booking != null) {
                        showBookingSuccessDialog(booking, station.name)
                    } else {
                        Toast.makeText(requireContext(), "Booking created but no details returned", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Failed to create booking"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                Log.e("CreateBooking", "Error creating booking", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBookingSuccessDialog(booking: Booking, stationName: String) {
        // Use professional layout instead of simple message dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_booking_success, null)
        
        // Find views and set data
        val bookingId = dialogView.findViewById<TextView>(R.id.textViewBookingId)
        val stationNameView = dialogView.findViewById<TextView>(R.id.textViewStationName)
        val bookingDate = dialogView.findViewById<TextView>(R.id.textViewBookingDate)
        val timeSlot = dialogView.findViewById<TextView>(R.id.textViewTimeSlot)
        val bookingStatus = dialogView.findViewById<TextView>(R.id.textViewBookingStatus)
        
        bookingId.text = booking.id
        stationNameView.text = stationName
        bookingDate.text = booking.reservationDate
        timeSlot.text = "${booking.reservationHour}:00 - ${booking.reservationHour + 1}:00"
        
        // Enhanced status styling with proper visibility and colors
        bookingStatus?.let { status ->
            Log.d("BookingSuccess", "Setting booking status: ${booking.status}")
            
            // Ensure visibility and proper styling
            status.visibility = View.VISIBLE
            status.textSize = 14f
            status.setPadding(16, 8, 16, 8)
            
            val statusText = booking.status.uppercase()
            status.text = statusText
            
            // Apply appropriate colors based on booking status
            when (statusText) {
                "PENDING" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_pending_background)
                    Log.d("BookingSuccess", "Applied PENDING styling")
                }
                "APPROVED", "CONFIRMED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_active_background)
                    Log.d("BookingSuccess", "Applied APPROVED/CONFIRMED styling")
                }
                "CANCELLED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_cancelled_background)
                    Log.d("BookingSuccess", "Applied CANCELLED styling")
                }
                "COMPLETED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_completed_background)
                    Log.d("BookingSuccess", "Applied COMPLETED styling")
                }
                else -> {
                    // Default styling for unknown status
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_active_background)
                    Log.d("BookingSuccess", "Applied default styling for status: $statusText")
                }
            }
            
            // Force layout update
            status.requestLayout()
            status.invalidate()
            
            Log.d("BookingSuccess", "Final booking status: '${status.text}', visibility: ${status.visibility}")
        } ?: Log.e("BookingSuccess", "bookingStatus TextView is null!")
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("View on Map") { dialog, _ ->
                dialog.dismiss()
                // Keep the station visible on map
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
                // Show all stations again
                displayStationsOnMap()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when fragment becomes visible
        Log.d("EVOwnerDashboard", "Fragment resumed, refreshing dashboard data")
        LoadingManager.show(requireContext(), "Refreshing dashboard...")
        loadUserProfileAndDashboard()
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
