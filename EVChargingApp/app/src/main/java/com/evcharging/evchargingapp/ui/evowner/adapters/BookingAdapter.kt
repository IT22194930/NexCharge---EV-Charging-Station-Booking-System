package com.evcharging.evchargingapp.ui.evowner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.databinding.ItemBookingBinding
import com.evcharging.evchargingapp.utils.DateTimeUtils

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onDeleteClick: (Booking) -> Unit,
    private val onViewQRClick: (Booking) -> Unit,
    private val getStationName: (String?) -> String
) : ListAdapter<Booking, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                // Set booking details
                textViewStationName.text = getStationName(booking.stationId)
                textViewStatus.text = booking.status.uppercase()
                textViewDateTime.text = DateTimeUtils.formatRelative(booking.reservationDate)
               

                // Set status color
                val statusColor = when (booking.status.lowercase()) {
                    "approved" -> android.graphics.Color.parseColor("#4CAF50") // Green for approved
                    "pending" -> android.graphics.Color.parseColor("#FF9800")  // Orange for pending
                    "cancelled" -> android.graphics.Color.parseColor("#F44336") // Red for cancelled
                    "completed" -> android.graphics.Color.parseColor("#2196F3") // Blue for completed
                    else -> android.graphics.Color.parseColor("#757575") // Gray for unknown
                }
                textViewStatus.setBackgroundColor(statusColor)

                // Set click listeners
                root.setOnClickListener { onBookingClick(booking) }
                buttonViewQR.setOnClickListener { onViewQRClick(booking) }
                buttonDelete.setOnClickListener { onDeleteClick(booking) }

                // Show/hide View QR button based on QR code availability
                buttonViewQR.visibility = if (!booking.qrBase64.isNullOrEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Show different information based on booking status
                when (booking.status.lowercase()) {
                    "pending" -> {
                        textViewCustomerName.text = "Status: Waiting for approval"
                    }
                    "approved" -> {
                        textViewCustomerName.text = "Status: Approved - Ready to charge"
                    }
                    "completed" -> {
                        textViewCustomerName.text = "Status: Charging completed"
                    }
                    "cancelled" -> {
                        textViewCustomerName.text = "Status: Booking cancelled"
                    }
                    else -> {
                        textViewCustomerName.text = "Status: ${booking.status}"
                    }
                }
            }
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}
