package com.example.healplusapp.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SearchHelper {
    
    fun EditText.textChanges(): Flow<String> = callbackFlow {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                trySend(s?.toString() ?: "")
            }
        }
        addTextChangedListener(textWatcher)
        awaitClose { removeTextChangedListener(textWatcher) }
    }
    
    fun filterList(query: String, items: List<String>): List<String> {
        if (query.isBlank()) return items
        val lowerQuery = query.lowercase()
        return items.filter { it.lowercase().contains(lowerQuery) }
    }
    
    fun <T> filterList(
        query: String,
        items: List<T>,
        filterFunction: (T, String) -> Boolean
    ): List<T> {
        if (query.isBlank()) return items
        val lowerQuery = query.lowercase()
        return items.filter { filterFunction(it, lowerQuery) }
    }
}

