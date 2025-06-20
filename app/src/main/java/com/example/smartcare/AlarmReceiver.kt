package com.example.smartcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val taskTitle = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Anda memiliki tugas baru!"

        // --- BARU: Buat Intent untuk membuka aplikasi saat notifikasi diklik ---
        val activityIntent = Intent(context, ElderlyDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // --------------------------------------------------------------------

        // Buat channel notifikasi (wajib untuk Android 8.0+)
        val channelId = "smartcare_channel"
        val channelName = "SmartCare Reminders"
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // Buat notifikasi
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pengingat Tugas")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // BARU: Tambahkan aksi klik di sini
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}