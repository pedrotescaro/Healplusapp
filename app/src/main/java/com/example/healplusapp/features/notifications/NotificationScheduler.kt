package com.example.healplusapp.features.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.healplusapp.features.agenda.model.Agendamento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleAgendamentoReminder(agendamento: Agendamento, hoursBefore: Int = 24) {
        val reminderTime = calculateReminderTime(agendamento, hoursBefore)
        if (reminderTime <= System.currentTimeMillis()) return
        
        val intent = Intent(context, AgendamentoReminderReceiver::class.java).apply {
            putExtra("agendamento_id", agendamento.id)
            putExtra("data", agendamento.dataAgendamento)
            putExtra("hora", agendamento.horaAgendamento)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            agendamento.id?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }
    
    fun cancelAgendamentoReminder(agendamentoId: Long) {
        val intent = Intent(context, AgendamentoReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            agendamentoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    private fun calculateReminderTime(agendamento: Agendamento, hoursBefore: Int): Long {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateTimeString = "${agendamento.dataAgendamento} ${agendamento.horaAgendamento ?: "00:00"}"
            val agendamentoTime = dateFormat.parse(dateTimeString)?.time ?: return 0
            
            val reminderTime = agendamentoTime - (hoursBefore * 60 * 60 * 1000)
            reminderTime
        } catch (e: Exception) {
            0
        }
    }
}

