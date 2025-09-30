package com.evcharging.evchargingapp.ui.evowner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Station
import com.google.android.material.button.MaterialButton

class StationsAdapter(
    private val onStationBookClick: (Station) -> Unit
) : ListAdapter<Station, StationsAdapter.StationViewHolder>(StationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station_card, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewStationName: TextView = itemView.findViewById(R.id.textViewStationName)
        private val textViewStationLocation: TextView = itemView.findViewById(R.id.textViewStationLocation)
        private val textViewStationType: TextView = itemView.findViewById(R.id.textViewStationType)
        private val textViewAvailableSlots: TextView = itemView.findViewById(R.id.textViewAvailableSlots)
        private val buttonBookStation: MaterialButton = itemView.findViewById(R.id.buttonBookStation)

        fun bind(station: Station) {
            textViewStationName.text = station.name
            textViewStationLocation.text = station.location
            textViewStationType.text = station.type
            textViewAvailableSlots.text = "${station.availableSlots} available"
            
            // Set click listener for book button
            buttonBookStation.setOnClickListener {
                onStationBookClick(station)
            }
            
            // Set click listener for the entire card
            itemView.setOnClickListener {
                onStationBookClick(station)
            }
            
            // Update button state based on availability
            if (station.availableSlots > 0) {
                buttonBookStation.isEnabled = true
                buttonBookStation.text = "Book"
                buttonBookStation.alpha = 1.0f
            } else {
                buttonBookStation.isEnabled = false
                buttonBookStation.text = "Full"
                buttonBookStation.alpha = 0.6f
            }
        }
    }

    class StationDiffCallback : DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem == newItem
        }
    }
}