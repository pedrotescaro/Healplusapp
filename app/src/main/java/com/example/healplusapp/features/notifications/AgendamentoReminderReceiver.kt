package com.example.healplusapp.features.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healplusapp.features.agenda.model.Agendamento

class AgendamentoReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val agendamentoId = intent.getLongExtra("agendamento_id", 0)
        val data = intent.getStringExtra("data") ?: ""
        val hora = intent.getStringExtra("hora") ?: ""
        val hoursBefore = intent.getIntExtra("hours_before", 24)
        
        val agendamento = Agendamento(
            id = agendamentoId,
            dataAgendamento = data,
            horaAgendamento = hora
        )
        
        val notificationService = NotificationService(context)
        notificationService.showAgendamentoReminder(agendamento, hoursBefore)
    }
}

