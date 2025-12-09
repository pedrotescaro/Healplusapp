package com.example.healplusapp.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healplusapp.MainActivity
import com.example.healplusapp.R
import com.example.healplusapp.features.agenda.model.Agendamento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationService(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de Consultas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de lembretes de consultas e agendamentos"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showAgendamentoReminder(agendamento: Agendamento, hoursBefore: Int = 24) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            agendamento.id?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_image)
            .setContentTitle("Lembrete de Consulta")
            .setContentText("Consulta em ${hoursBefore}h: ${agendamento.dataAgendamento} ${agendamento.horaAgendamento ?: ""}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Você tem uma consulta agendada para ${agendamento.dataAgendamento} às ${agendamento.horaAgendamento ?: "horário não definido"}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(agendamento.id?.toInt() ?: 0, notification)
    }
    
    fun showDailyAgendamentos(agendamentos: List<Agendamento>) {
        if (agendamentos.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            DAILY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (agendamentos.size == 1) {
            "Você tem 1 consulta hoje"
        } else {
            "Você tem ${agendamentos.size} consultas hoje"
        }
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Consultas de Hoje")
        
        agendamentos.take(5).forEach { agendamento ->
            inboxStyle.addLine("${agendamento.dataAgendamento} - ${agendamento.horaAgendamento ?: ""}")
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_image)
            .setContentTitle("Consultas de Hoje")
            .setContentText(contentText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(DAILY_NOTIFICATION_ID, notification)
    }
    
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
    
    companion object {
        private const val CHANNEL_ID = "healplus_agendamentos"
        private const val DAILY_NOTIFICATION_ID = 1000
    }
}

