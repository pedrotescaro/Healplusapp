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
            putExtra("agendamento_id", agendamento.id ?: 0L)
            putExtra("data", agendamento.dataAgendamento)
            putExtra("hora", agendamento.horaAgendamento)
        }
        
        // ID único para cada tipo de lembrete (24h ou 1h)
        val requestCode = ((agendamento.id ?: 0L) * 100 + hoursBefore).toInt()
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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
        // Cancela ambos os lembretes (24h e 1h)
        val intent = Intent(context, AgendamentoReminderReceiver::class.java)
        
        // Cancela lembrete de 24h
        val pendingIntent24h = PendingIntent.getBroadcast(
            context,
            (agendamentoId * 100 + 24).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent24h)
        
        // Cancela lembrete de 1h
        val pendingIntent1h = PendingIntent.getBroadcast(
            context,
            (agendamentoId * 100 + 1).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent1h)
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

