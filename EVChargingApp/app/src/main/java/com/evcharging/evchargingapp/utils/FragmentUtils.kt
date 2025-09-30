package com.evcharging.evchargingapp.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * Extension functions for safer fragment operations
 */

/**
 * Safely load a fragment, checking activity state to prevent IllegalStateException
 */
fun FragmentActivity.safeLoadFragment(
    containerId: Int,
    fragment: Fragment,
    addToBackStack: Boolean = false,
    tag: String? = null
) {
    if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
        
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        
        transaction.commitAllowingStateLoss()
    }
}

/**
 * Safely add a fragment, checking activity state to prevent IllegalStateException
 */
fun FragmentActivity.safeAddFragment(
    containerId: Int,
    fragment: Fragment,
    addToBackStack: Boolean = false,
    tag: String? = null
) {
    if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
        val transaction = supportFragmentManager.beginTransaction()
            .add(containerId, fragment, tag)
        
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        
        transaction.commitAllowingStateLoss()
    }
}

/**
 * Check if it's safe to perform fragment transactions
 */
fun FragmentActivity.isSafeForFragmentTransactions(): Boolean {
    return !isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved
}