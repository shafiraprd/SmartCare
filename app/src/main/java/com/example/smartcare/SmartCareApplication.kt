package com.example.smartcare

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SmartCareApplication : Application() {

    companion object {
        // ID channel notifikasi yang nanti akan dipakai saat membuat notifikasi
        const val CHANNEL_ID = "smartcare_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        // panggil fungsi untuk membuat notification channel saat aplikasi pertama kali dijalankan
        createNotificationChannel()
    }

    // fungsi ini membuat notification channel yang dibutuhkan untuk Android 8.0 ke atas
    private fun createNotificationChannel() {
        // hanya dijalankan jika versi Android adalah Oreo (API 26) atau lebih tinggi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Jadwal" // nama channel yang akan muncul di pengaturan sistem
            val descriptionText = "Notifikasi untuk jadwal dan kegiatan" // deskripsi channel
            val importance = NotificationManager.IMPORTANCE_HIGH // tingkat urgensi notifikasi

            // buat objek NotificationChannel
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // daftarkan channel ke sistem Android supaya bisa digunakan
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
