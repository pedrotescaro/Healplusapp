package com.example.healplusapp.utils

import android.view.View
import android.widget.TextView
import com.example.healplusapp.R

object EmptyStateHelper {
    
    fun setupEmptyState(
        emptyStateView: View?,
        title: String? = null,
        message: String? = null
    ) {
        emptyStateView?.let { view ->
            title?.let {
                view.findViewById<TextView>(R.id.empty_title)?.text = it
            }
            message?.let {
                view.findViewById<TextView>(R.id.empty_message)?.text = it
            }
        }
    }
    
    fun showEmptyState(
        emptyStateView: View?,
        recyclerView: View?,
        title: String? = null,
        message: String? = null
    ) {
        setupEmptyState(emptyStateView, title, message)
        emptyStateView?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
    }
    
    fun hideEmptyState(emptyStateView: View?, recyclerView: View?) {
        emptyStateView?.visibility = View.GONE
        recyclerView?.visibility = View.VISIBLE
    }
}

