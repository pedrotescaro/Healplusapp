package com.example.healplusapp.utils

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.Fragment
import com.example.healplusapp.R

object DialogHelper {
    
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirmar") { _, _ -> onConfirm() }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    fun showDeleteConfirmDialog(
        context: Context,
        itemName: String,
        onConfirm: () -> Unit
    ) {
        showConfirmDialog(
            context,
            "Confirmar exclusão",
            "Tem certeza que deseja excluir \"$itemName\"? Esta ação não pode ser desfeita.",
            onConfirm
        )
    }
    
    fun showArchiveConfirmDialog(
        context: Context,
        itemName: String,
        onConfirm: () -> Unit
    ) {
        showConfirmDialog(
            context,
            "Confirmar arquivamento",
            "Deseja arquivar \"$itemName\"? Você poderá desarquivar depois.",
            onConfirm
        )
    }
    
    fun Fragment.showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        DialogHelper.showConfirmDialog(requireContext(), title, message, onConfirm)
    }
    
    fun Fragment.showDeleteConfirmDialog(
        itemName: String,
        onConfirm: () -> Unit
    ) {
        DialogHelper.showDeleteConfirmDialog(requireContext(), itemName, onConfirm)
    }
    
    fun Fragment.showArchiveConfirmDialog(
        itemName: String,
        onConfirm: () -> Unit
    ) {
        DialogHelper.showArchiveConfirmDialog(requireContext(), itemName, onConfirm)
    }
}

