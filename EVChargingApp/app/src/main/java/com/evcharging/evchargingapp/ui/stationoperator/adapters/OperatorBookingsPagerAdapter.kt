package com.evcharging.evchargingapp.ui.stationoperator.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.evcharging.evchargingapp.ui.stationoperator.fragments.tabs.ApprovedBookingsTabFragment
import com.evcharging.evchargingapp.ui.stationoperator.fragments.tabs.CompletedBookingsTabFragment

class OperatorBookingsPagerAdapter(private val fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ApprovedBookingsTabFragment.newInstance()
            1 -> CompletedBookingsTabFragment.newInstance()
            else -> ApprovedBookingsTabFragment.newInstance()
        }
    }

    fun getActiveFragment(position: Int): Fragment? {
        return fragmentActivity.supportFragmentManager.findFragmentByTag("f$position")
    }
    
    fun filterBookingsInAllTabs(query: String) {
        // Filter in approved tab
        val approvedFragment = getActiveFragment(0) as? ApprovedBookingsTabFragment
        approvedFragment?.filterBookings(query)
        
        // Filter in completed tab
        val completedFragment = getActiveFragment(1) as? CompletedBookingsTabFragment
        completedFragment?.filterBookings(query)
    }
    
    fun refreshAllTabs() {
        // Refresh approved tab
        val approvedFragment = getActiveFragment(0) as? ApprovedBookingsTabFragment
        approvedFragment?.refreshBookings()
        
        // Refresh completed tab
        val completedFragment = getActiveFragment(1) as? CompletedBookingsTabFragment
        completedFragment?.refreshBookings()
    }
}