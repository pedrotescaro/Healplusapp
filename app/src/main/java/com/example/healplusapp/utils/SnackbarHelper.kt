package com.example.healplusapp.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar

object SnackbarHelper {
    
    fun showSuccess(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(view.context.getColor(android.R.color.holo_green_dark))
            .show()
    }
    
    fun showError(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(view.context.getColor(android.R.color.holo_red_dark))
            .show()
    }
    
    fun showInfo(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(view.context.getColor(android.R.color.holo_blue_dark))
            .show()
    }
    
    fun showWithAction(
        view: View,
        message: String,
        actionLabel: String,
        action: () -> Unit
    ) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setAction(actionLabel) { action() }
            .show()
    }
}

