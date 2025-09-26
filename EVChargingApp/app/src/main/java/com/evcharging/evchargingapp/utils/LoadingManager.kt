package com.evcharging.evchargingapp.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.evcharging.evchargingapp.R

class LoadingManager {
    
    companion object {
        private var loadingDialog: Dialog? = null
        
        /**
         * Show a loading dialog with default message
         */
        fun show(context: Context) {
            show(context, "Loading...")
        }
        
        /**
         * Show a loading dialog with custom message
         */
        fun show(context: Context, message: String) {
            try {
                // Dismiss any existing dialog first
                dismiss()
                
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
                val messageTextView = dialogView.findViewById<TextView>(R.id.textViewLoadingMessage)
                messageTextView.text = message
                
                loadingDialog = Dialog(context, R.style.TransparentDialog).apply {
                    setContentView(dialogView)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    
                    // Set dialog window properties for better appearance
                    window?.apply {
                        setLayout(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT
                        )
                        setDimAmount(0.3f)
                    }
                }
                
                loadingDialog?.show()
            } catch (e: Exception) {
                // Handle case where context might be invalid
                e.printStackTrace()
            }
        }
        
        /**
         * Dismiss the loading dialog
         */
        fun dismiss() {
            try {
                loadingDialog?.let { dialog ->
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }
                loadingDialog = null
            } catch (e: Exception) {
                // Handle case where dialog might be in invalid state
                e.printStackTrace()
            }
        }
        
        /**
         * Check if loading dialog is currently showing
         */
        fun isShowing(): Boolean {
            return loadingDialog?.isShowing ?: false
        }
        
        /**
         * Update the loading message without dismissing the dialog
         */
        fun updateMessage(message: String) {
            try {
                loadingDialog?.let { dialog ->
                    if (dialog.isShowing) {
                        val messageTextView = dialog.findViewById<TextView>(R.id.textViewLoadingMessage)
                        messageTextView?.text = message
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}