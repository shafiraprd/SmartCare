package com.example.smartcare

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Ada jadwal baru!"
        val taskId = intent.getIntExtra("EXTRA_TASK_ID", 0)

        // Intent untuk membuka aplikasi saat notifikasi di-klik
        val mainIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, taskId, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        // Membuat notifikasi
        val builder = NotificationCompat.Builder(context, SmartCareApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ikon notifikasi (lihat langkah terakhir)
            .setContentTitle("Pengingat Jadwal SmartCare")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Notifikasi hilang saat di-klik

        // Tampilkan notifikasi
        with(NotificationManagerCompat.from(context)) {
            // Izin sudah diminta di ElderlyDashboardActivity
            notify(taskId, builder.build())
        }
    }
}