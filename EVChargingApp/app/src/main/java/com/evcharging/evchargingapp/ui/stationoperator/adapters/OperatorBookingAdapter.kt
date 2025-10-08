package com.evcharging.evchargingapp.ui.stationoperator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class OperatorBookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onCompleteClick: ((Booking) -> Unit)? = null,
    private val getStationName: (String?) -> String
) : RecyclerView.Adapter<OperatorBookingAdapter.BookingViewHolder>() {

    private var bookings = listOf<Booking>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operator_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardViewBooking)
        private val textViewBookingId: TextView = itemView.findViewById(R.id.textViewBookingId)
    private val textViewStationName: TextView = itemView.findViewById(R.id.textViewStationName)
    private val textViewDateTime: TextView = itemView.findViewById(R.id.textViewDateTime)
        private val textViewCustomerNic: TextView = itemView.findViewById(R.id.textViewCustomerNic)
        private val buttonComplete: MaterialButton = itemView.findViewById(R.id.buttonComplete)

        fun bind(booking: Booking) {
            textViewBookingId.text = "ID: ${booking.id}"
            textViewStationName.text = getStationName(booking.stationId)
            textViewDateTime.text = DateTimeUtils.formatDateTimeWithHour(booking.reservationDate, booking.reservationHour)
            textViewCustomerNic.text = "Customer: ${booking.ownerNic}"

            // Status pill removed from layout; keep card stroke changes for visual cue below

            // Show/hide complete button based on status and callback
            if (booking.status.equals("Approved", ignoreCase = true) && onCompleteClick != null) {
                buttonComplete.visibility = View.VISIBLE
                buttonComplete.setOnClickListener {
                    onCompleteClick.invoke(booking)
                }
            } else {
                buttonComplete.visibility = View.GONE
            }

            // Set click listener for entire card
            cardView.setOnClickListener {
                onBookingClick(booking)
            }

            // Add visual distinction for different statuses
            when (booking.status.lowercase()) {
                "approved" -> {
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.status_approved)
                    cardView.strokeWidth = 2
                }
                "completed" -> {
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.status_completed)
                    cardView.strokeWidth = 2
                }
                else -> {
                    cardView.strokeWidth = 0
                }
            }
        }
    }
}