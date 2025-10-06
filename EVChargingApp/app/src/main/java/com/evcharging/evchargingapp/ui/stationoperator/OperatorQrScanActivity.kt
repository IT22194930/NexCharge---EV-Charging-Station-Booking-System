package com.evcharging.evchargingapp.ui.stationoperator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.model.UserProfile
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class OperatorQrScanActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar

    private var processing = false
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var operatorStationId: String? = null

    private var operatorStationName: String? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_qr_scan)

        previewView = findViewById(R.id.previewView)
        progressBar = findViewById(R.id.progressBar)

        // Get operator's assigned station first
        scope.launch {
            getCurrentUserProfile()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            Log.d("OperatorQrScan","CameraProvider obtained")
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(cameraExecutor) { image ->
                if (!processing) processImage(image)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)
                Log.d("OperatorQrScan","Camera bound to lifecycle")
            } catch (e: Exception) {
                Log.e("OperatorQrScan", "Camera binding failed", e)
                runOnUiThread { Toast.makeText(this, "Camera start failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun yuvToLuminance(buffer: ByteBuffer, width: Int, height: Int): ByteArray {
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data.sliceArray(0 until width * height) // Use only Y plane
    }

    private fun processImage(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            try {
                val buffer = mediaImage.planes[0].buffer
                val bytes = yuvToLuminance(buffer, image.width, image.height)
                val source = PlanarYUVLuminanceSource(bytes, image.width, image.height, 0, 0, image.width, image.height, false)
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                val result = reader.decodeWithState(bitmap)
                Log.d("OperatorQrScan","QR detected raw=${result.text}")
                if (result.text.startsWith("booking:")) {
                    processing = true
                    val parts = result.text.split("|")
                    val bookingId = parts.firstOrNull { it.startsWith("booking:") }?.substringAfter(":")
                    if (bookingId != null) {
                        Log.d("OperatorQrScan","Parsed bookingId=$bookingId")
                        scope.launch { fetchBooking(bookingId) }
                    } else {
                        Log.w("OperatorQrScan","bookingId not found in QR string")
                        processing = false
                    }
                }
            } catch (e: NotFoundException) {
                // no QR in frame
            } catch (e: Exception) {
                Log.e("OperatorQrScan", "Decode error", e)
            } finally {
                image.close()
            }
        } else {
            image.close()
        }
    }

    private suspend fun getCurrentUserProfile() {
        try {
            val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
            val response = withContext(Dispatchers.IO) { api.getCurrentUserProfile() }
            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                operatorStationId = userProfile.assignedStationId
                operatorStationName = userProfile.assignedStationName
                Log.d("OperatorQrScan", "Operator assigned to station: $operatorStationName (ID: $operatorStationId)")
            } else {
                Log.e("OperatorQrScan", "Failed to load user profile: ${response.code()}")
                showError("Failed to load operator profile. Please try again.")
            }
        } catch (e: Exception) {
            Log.e("OperatorQrScan", "Error loading user profile", e)
            showError("Error loading operator profile: ${e.message}")
        }
    }

    private suspend fun fetchBooking(id: String) {
        withContext(Dispatchers.Main) { progressBar.visibility = View.VISIBLE }
        try {
            val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
            val response = withContext(Dispatchers.IO) { api.getBookingById(id) }
            if (response.isSuccessful) {
                val booking = response.body()
                validateStationAssignment(booking)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = when (response.code()) {
                    401 -> "Unauthorized – token invalid/expired"
                    403 -> "Forbidden – role not permitted"
                    404 -> "Booking not found (id=$id)"
                    else -> "HTTP ${response.code()} ${response.message()}"
                }
                showError("$msg\n${errorBody ?: ""}".trim())
            }
        } catch (e: Exception) {
            showError("Network error: ${e.message}")
        } finally {
            withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
        }
    }

    private fun validateStationAssignment(booking: Booking?) {
        if (booking == null) { 
            showError("Invalid booking data")
            return 
        }

        // Check if operator has an assigned station
        if (operatorStationId.isNullOrEmpty()) {
            showError("⚠️ Station Assignment Error\n\nYou are not assigned to any station. Please contact your administrator to assign you to a station before scanning QR codes.")
            return
        }

        // Check if the booking belongs to the operator's assigned station
        if (booking.stationId != operatorStationId) {
            showStationMismatchError(booking)
            return
        }

        // If validation passes, show the booking details
        showReservationDetailsDialog(booking)
    }

    private fun showStationMismatchError(booking: Booking) {
        runOnUiThread {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error_station_mismatch, null)
                ?: LayoutInflater.from(this).inflate(android.R.layout.select_dialog_singlechoice, null) // Fallback

            // Try to find custom views, fall back to basic layout if not available
            val titleText = dialogView.findViewById<TextView>(R.id.titleText) 
                ?: dialogView.findViewById<TextView>(android.R.id.title)
            val messageText = dialogView.findViewById<TextView>(R.id.messageText)
                ?: dialogView.findViewById<TextView>(android.R.id.text1)
            val buttonOk = dialogView.findViewById<MaterialButton>(R.id.buttonOk)

            titleText?.text = "❌ Station Assignment Error"
            
            // Handle null or empty station name by showing station ID and fetching name if possible
            val bookingStationDisplay = if (booking.stationName.isNullOrEmpty()) {
                "Station ID: ${booking.stationId} (Loading name...)"
            } else {
                booking.stationName
            }
            
            val message = "This booking is not assigned to your station.\n\n" +
                    "• Your assigned station: $operatorStationName\n" +
                    "• Booking station: $bookingStationDisplay\n" +
                    "• Booking ID: ${booking.id}\n\n" +
                    "You can only scan QR codes for bookings at your assigned station. " +
                    "Please direct the customer to the correct station or contact your administrator if there's an error."
            
            messageText?.text = message
            
            // If station name is null/empty, fetch it in background and update the message
            if (booking.stationName.isNullOrEmpty()) {
                scope.launch {
                    fetchStationNameAndUpdateDialog(booking.stationId, messageText)
                }
            }

            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Set up OK button click listener
            buttonOk?.setOnClickListener {
                dialog.dismiss()
                processing = false // Allow scanning again
            } ?: run {
                // Fallback for basic dialog
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
                    processing = false
                }
            }

            dialog.setOnDismissListener {
                processing = false // Allow scanning again when dialog is dismissed
            }

            dialog.show()

            // Fallback toast message if dialog creation fails
            if (titleText == null && messageText == null) {
                val bookingStationDisplay = booking.stationName ?: "Station ID: ${booking.stationId}"
                Toast.makeText(this, 
                    "❌ Station Assignment Error: This booking (${booking.id}) is for station $bookingStationDisplay, but you are assigned to station $operatorStationName. Please scan QR codes only for your assigned station.",
                    Toast.LENGTH_LONG).show()
                processing = false
            }
        }
    }

    private fun showReservationDetailsDialog(booking: Booking?) {
        if (booking == null) { 
            showError("Invalid booking data")
            return 
        }

        runOnUiThread {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reservation_details, null)
            
            // Debug logging
            Log.d("OperatorQrScan", "Booking data: id=${booking.id}, ownerNic='${booking.ownerNic}', stationId=${booking.stationId}, status=${booking.status}")
            
            // Find views in dialog
            val textViewBookingId = dialogView.findViewById<TextView>(R.id.textViewBookingId)
            val textViewOwnerNic = dialogView.findViewById<TextView>(R.id.textViewOwnerNic)
            val textViewStationName = dialogView.findViewById<TextView>(R.id.textViewStationName)
            val textViewDateTime = dialogView.findViewById<TextView>(R.id.textViewDateTime)
            val textViewStatus = dialogView.findViewById<TextView>(R.id.textViewStatus)
            val statusIndicator = dialogView.findViewById<View>(R.id.statusIndicator)
            val buttonClose = dialogView.findViewById<MaterialButton>(R.id.buttonClose)
            val buttonComplete = dialogView.findViewById<MaterialButton>(R.id.buttonComplete)

            // Populate dialog with booking data
            textViewBookingId.text = booking.id
            textViewOwnerNic.text = if (booking.ownerNic.isNotEmpty()) booking.ownerNic else "N/A"
            
            // Display station information - show both ID and name
            if (!booking.stationName.isNullOrEmpty()) {
                textViewStationName.text = "Station ID: ${booking.stationId}\nStation Name: ${booking.stationName}"
            } else {
                // If station name is not available, fetch it from API
                textViewStationName.text = "Loading station info..."
                scope.launch { fetchAndDisplayStationInfo(booking.stationId, textViewStationName) }
            }
            
            textViewDateTime.text = formatDateTime(booking.reservationDate, booking.reservationHour)
            textViewStatus.text = "Status: ${booking.status.uppercase()}"

            // Set status indicator color based on booking status
            val statusColor = when (booking.status.lowercase()) {
                "approved" -> ContextCompat.getColor(this, R.color.nexcharge_success)
                "pending" -> ContextCompat.getColor(this, R.color.nexcharge_warning)
                "cancelled" -> ContextCompat.getColor(this, R.color.nexcharge_error)
                "completed" -> ContextCompat.getColor(this, R.color.nexcharge_primary)
                else -> ContextCompat.getColor(this, R.color.nexcharge_text_secondary)
            }
            statusIndicator.setBackgroundColor(statusColor)

            // Find the confirm button that should be added to the dialog layout
            val buttonConfirm = dialogView.findViewById<MaterialButton>(R.id.buttonConfirm)

            // Enable/disable buttons based on status
            when (booking.status.lowercase()) {
                "approved" -> {
                    buttonConfirm.isEnabled = true
                    buttonComplete.isEnabled = false
                    buttonConfirm.text = "Confirm Arrival"
                }
                "started" -> {
                    buttonConfirm.isEnabled = false
                    buttonComplete.isEnabled = true
                    buttonConfirm.text = "Already Started"
                }
                "completed" -> {
                    buttonConfirm.isEnabled = false
                    buttonComplete.isEnabled = false
                    buttonConfirm.text = "Completed"
                }
                else -> {
                    buttonConfirm.isEnabled = false
                    buttonComplete.isEnabled = false
                }
            }
            
            // Create and show dialog
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Set transparent background for rounded corners
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Button click listeners
            buttonClose.setOnClickListener {
                dialog.dismiss()
                processing = false // Allow scanning again
            }

            buttonConfirm.setOnClickListener {
                dialog.dismiss()
                confirmBooking(booking.id)
            }

            buttonComplete.setOnClickListener {
                dialog.dismiss()
                completeBooking(booking.id)
            }

            dialog.setOnDismissListener {
                processing = false // Allow scanning again when dialog is dismissed
            }

            dialog.show()
        }
    }

    private suspend fun fetchStationNameAndUpdateDialog(stationId: String, messageTextView: TextView?) {
        try {
            val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
            val response = withContext(Dispatchers.IO) { api.getStationById(stationId) }
            if (response.isSuccessful) {
                val station = response.body()
                withContext(Dispatchers.Main) {
                    if (station != null && messageTextView != null) {
                        val currentText = messageTextView.text.toString()
                        val updatedText = currentText.replace(
                            "Station ID: $stationId (Loading name...)",
                            station.name
                        )
                        messageTextView.text = updatedText
                        Log.d("OperatorQrScan", "Updated station name in dialog: ${station.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OperatorQrScan", "Error fetching station name for dialog", e)
        }
    }

    private suspend fun fetchAndDisplayStationInfo(stationId: String, textView: TextView) {
        try {
            val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
            val response = withContext(Dispatchers.IO) { api.getStationById(stationId) }
            if (response.isSuccessful) {
                val station = response.body()
                withContext(Dispatchers.Main) {
                    if (station != null) {
                        textView.text = "${station.name}\nID: ${station.id}"
                        Log.d("OperatorQrScan", "Station info loaded: ${station.name} (${station.id})")
                    } else {
                        textView.text = "Station ID: $stationId\n(Name not available)"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    textView.text = "Station ID: $stationId\n(Unable to load name)"
                    Log.w("OperatorQrScan", "Failed to fetch station info: ${response.code()} ${response.message()}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                textView.text = "Station ID: $stationId\n(Error loading name)"
                Log.e("OperatorQrScan", "Error fetching station info", e)
            }
        }
    }

    private fun formatDateTime(reservationDate: String, reservationHour: Int): String {
        return try {
            // Format the date and time for display
            val datePart = reservationDate.split("T")[0] // Extract date part if it's in ISO format
            "$datePart at ${reservationHour}:00 - ${reservationHour + 1}:00"
        } catch (e: Exception) {
            "$reservationDate at ${reservationHour}:00 - ${reservationHour + 1}:00"
        }
    }

    private fun showError(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            processing = false
        }
    }

    private fun confirmBooking(id: String) {
        scope.launch {
            withContext(Dispatchers.Main) { progressBar.visibility = View.VISIBLE }
            try {
                val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
                val response = withContext(Dispatchers.IO) { api.confirmBooking(id) }
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OperatorQrScanActivity, "✅ Arrival confirmed successfully! Charging session started.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val body = response.errorBody()?.string()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OperatorQrScanActivity, "Failed to confirm: ${response.code()} ${response.message()} ${body ?: ""}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OperatorQrScanActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }

    private fun completeBooking(id: String) {
        scope.launch {
            withContext(Dispatchers.Main) { progressBar.visibility = View.VISIBLE }
            try {
                val api = RetrofitInstance.createApiService(this@OperatorQrScanActivity)
                val response = withContext(Dispatchers.IO) { api.completeBooking(id) }
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OperatorQrScanActivity, "✅ Booking marked as completed successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val body = response.errorBody()?.string()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OperatorQrScanActivity, "Failed: ${response.code()} ${response.message()} ${body ?: ""}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OperatorQrScanActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }
}
